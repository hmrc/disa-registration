/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.disaregistration.controllers

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import org.scalatestplus.mockito.MockitoSugar
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import play.api.test.Helpers.await
import play.api.{Application, inject}
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.journeyData.EnrolmentStatus.{Active, Submitted}
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaregistration.utils.WiremockHelper.stubPost
import uk.gov.hmrc.mongo.MongoComponent

class SubmissionControllerISpec extends BaseIntegrationSpec with MockitoSugar {

  private val databaseName: String                    = "disa-registration-submission-test"
  private lazy val mongoUri: String                   = s"mongodb://127.0.0.1:27017/$databaseName"
  private lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  override lazy val app: Application = app(inject.bind[MongoComponent].toInstance(mockMongoComponent))
  val repo: JourneyAnswersRepository = app.injector.instanceOf[JourneyAnswersRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    stubAuth()
    await(repo.collection.drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repo.collection.drop().toFuture())
  }

  "SubmissionController.declareAndSubmit" should {
    val url = s"http://localhost:$port/disa-registration/$testGroupId/declare-and-submit"

    "return 200 with formBundleId JSON and mark journey as Submitted when journey data exists" in {
      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testEnrolmentId,
        formBundleId = None,
        status = Active,
        businessVerification = Some(businessVerification),
        organisationDetails = Some(organisationDetails),
        thirdPartyOrganisations = None
      )
      await(repo.collection.insertOne(jd).toFuture())

      val etmpResponse = s"""
           | {"formBundleId": "$testFormBundleId"}
           | """.stripMargin
      stubPost(url = "/etmp/enrolment/submission", status = OK, responseBody = etmpResponse)

      val response = await(
        ws.url(url)
          .withHttpHeaders(testHeaders: _*)
          .post("")
      )

      response.status shouldBe OK
      response.json   shouldBe Json.toJson(EnrolmentSubmissionResponse(testFormBundleId))

      val stored = await(repo.collection.find(Filters.eq("groupId", testGroupId)).toFuture())
      stored.size              shouldBe 1
      stored.head.status       shouldBe Submitted
      stored.head.formBundleId shouldBe Some(testFormBundleId)

    }

    "subscribe to Tax Enrolments inline with the bearer token" in {
      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testEnrolmentId,
        formBundleId = None,
        status = Active,
        businessVerification = Some(businessVerification),
        organisationDetails = Some(organisationDetails),
        thirdPartyOrganisations = None
      )

      await(repo.collection.insertOne(jd).toFuture())

      val etmpResponse = s"""
                             | {"formBundleId": "$testFormBundleId"}
                             | """.stripMargin
      stubPost(url = "/etmp/enrolment/submission", status = OK, responseBody = etmpResponse)
      stubFor(
        put(urlEqualTo(s"/tax-enrolments/subscriptions/$testFormBundleId/subscriber"))
          .withHeader("Authorization", equalTo("Bearer mock-bearer-token"))
          .willReturn(aResponse().withStatus(play.api.http.Status.NO_CONTENT))
      )

      val response = await(
        ws.url(url)
          .withHttpHeaders(testHeaders: _*)
          .post("")
      )

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/tax-enrolments/subscriptions/$testFormBundleId/subscriber"))
          .withHeader("Authorization", equalTo("Bearer mock-bearer-token"))
          .withRequestBody(
            equalToJson(
              Json
                .obj(
                  "serviceName" -> "HMRC-DISA-ORG",
                  "callback"    -> s"http://localhost:11111/disa-registration/callback/subscriptions/$testFormBundleId",
                  "etmpId"      -> businessVerification.businessPartnerId.get
                )
                .toString
            )
          )
      )
      response.status shouldBe OK
      response.json   shouldBe Json.toJson(EnrolmentSubmissionResponse(testFormBundleId))

      val stored = await(repo.collection.find(Filters.eq("groupId", testGroupId)).toFuture())
      stored.size              shouldBe 1
      stored.head.status       shouldBe Submitted
      stored.head.formBundleId shouldBe Some(testFormBundleId)

    }

    "return 401 without calling downstream services for an unauthorised request" in {
      stubAuthFail()
      val response = await(ws.url(url).withHttpHeaders(testHeaders: _*).post(""))
      response.status shouldBe 401
      verify(0, postRequestedFor(urlEqualTo("/etmp/enrolment/submission")))
      verify(0, putRequestedFor(urlMatching("/tax-enrolments/.*")))
    }

    Seq(
      "organisation details"   -> testJourneyData.copy(organisationDetails = None),
      "correspondence address" -> testJourneyData.copy(organisationDetails =
        Some(organisationDetails.copy(correspondenceAddress = None))
      ),
      "business verification"  -> testJourneyData.copy(businessVerification = None),
      "business partner ID"    -> testJourneyData.copy(businessVerification =
        Some(businessVerification.copy(businessPartnerId = None))
      )
    ).foreach { case (missing, jd) =>
      s"return 500 without downstream calls when missing $missing" in {
        await(repo.collection.insertOne(jd).toFuture())
        val response = await(ws.url(url).withHttpHeaders(testHeaders: _*).post(""))
        response.status                              shouldBe INTERNAL_SERVER_ERROR
        await(repo.findById(testGroupId)).get.status shouldBe Active
        verify(0, postRequestedFor(urlEqualTo("/etmp/enrolment/submission")))
        verify(0, putRequestedFor(urlMatching("/tax-enrolments/.*")))
      }
    }

    Seq("server error", "invalid response", "connection failure").foreach { failure =>
      s"return 500 and preserve the Active journey on ETMP $failure" in {
        await(repo.collection.insertOne(testJourneyData).toFuture())
        val responseBuilder = failure match {
          case "server error"     => aResponse().withStatus(500)
          case "invalid response" => aResponse().withStatus(200).withBody("""{"unexpected":"value"}""")
          case _                  => aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
        }
        stubFor(post(urlEqualTo("/etmp/enrolment/submission")).willReturn(responseBuilder))
        val response        = await(ws.url(url).withHttpHeaders(testHeaders: _*).post(""))
        response.status shouldBe INTERNAL_SERVER_ERROR
        val stored = await(repo.findById(testGroupId)).get
        stored.status       shouldBe Active
        stored.formBundleId shouldBe None
        verify(0, putRequestedFor(urlMatching("/tax-enrolments/.*")))
      }
    }

    Seq("client error", "server error", "connection failure").foreach { failure =>
      s"return 200 and retain Submitted state on Tax Enrolments $failure" in {
        await(repo.collection.insertOne(testJourneyData).toFuture())
        stubPost("/etmp/enrolment/submission", OK, Json.obj("formBundleId" -> testFormBundleId).toString)
        val responseBuilder = failure match {
          case "client error" => aResponse().withStatus(400)
          case "server error" => aResponse().withStatus(500)
          case _              => aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
        }
        stubFor(
          put(urlEqualTo(s"/tax-enrolments/subscriptions/$testFormBundleId/subscriber")).willReturn(responseBuilder)
        )
        val response        = await(ws.url(url).withHttpHeaders(testHeaders: _*).post(""))
        response.status shouldBe OK
        response.json   shouldBe Json.toJson(EnrolmentSubmissionResponse(testFormBundleId))
        val stored = await(repo.findById(testGroupId)).get
        stored.status       shouldBe Submitted
        stored.formBundleId shouldBe Some(testFormBundleId)
        verify(putRequestedFor(urlEqualTo(s"/tax-enrolments/subscriptions/$testFormBundleId/subscriber")))
      }
    }

    "return 404 when journey data does not exist" in {
      val etmpResponse = s"""
                            | {"formBundleId": "$testFormBundleId"}
                            | """.stripMargin
      stubPost(url = "/etmp/enrolment/submission", status = OK, responseBody = etmpResponse)

      val response = await(
        ws.url(url)
          .withHttpHeaders(testHeaders: _*)
          .post("")
      )

      verify(0, postRequestedFor(urlEqualTo("/etmp/enrolment/submission")))
      verify(0, putRequestedFor(urlMatching("/tax-enrolments/.*")))
      response.status        shouldBe NOT_FOUND
      response.body.toString shouldBe "Failed to find journey data to submit for this request"
    }

    "return 500 when journey exists but no Active journey is available to submit" in {
      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
        organisationDetails = Some(organisationDetails),
        formBundleId = Some(testFormBundleId),
        status = Submitted,
        businessVerification = Some(businessVerification),
        thirdPartyOrganisations = None
      )

      await(repo.collection.insertOne(jd).toFuture())

      val etmpResponse = s"""
                            | {"formBundleId": "$testFormBundleId"}
                            | """.stripMargin
      stubPost(url = "/etmp/enrolment/submission", status = OK, responseBody = etmpResponse)

      val response = await(
        ws.url(url)
          .withHttpHeaders(testHeaders: _*)
          .post("")
      )

      verify(postRequestedFor(urlEqualTo("/etmp/enrolment/submission")))
      verify(0, putRequestedFor(urlMatching("/tax-enrolments/.*")))
      response.status        shouldBe INTERNAL_SERVER_ERROR
      response.body.toString shouldBe "There has been an issue processing your request"

      val stored = await(repo.collection.find(Filters.eq("groupId", testGroupId)).toFuture())
      stored.size              shouldBe 1
      stored.head.status       shouldBe Submitted
      stored.head.formBundleId shouldBe Some(testFormBundleId)
    }

    "return 500 when ETMP returns an error response" in {
      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testEnrolmentId,
        formBundleId = None,
        status = Active,
        organisationDetails = Some(organisationDetails),
        businessVerification = Some(businessVerification),
        thirdPartyOrganisations = None
      )

      await(repo.collection.insertOne(jd).toFuture())

      stubPost(
        url = "/etmp/enrolment/submission",
        status = 401,
        responseBody = """{"code":"UNAUTHORIZED","message":"Unauthorised"}"""
      )

      val response = await(
        ws.url(url)
          .withHttpHeaders(testHeaders: _*)
          .post("")
      )

      verify(postRequestedFor(urlEqualTo("/etmp/enrolment/submission")))
      verify(0, putRequestedFor(urlMatching("/tax-enrolments/.*")))
      response.status        shouldBe INTERNAL_SERVER_ERROR
      response.body.toString shouldBe "There has been an issue processing your request"

      val stored = await(repo.collection.find(Filters.eq("groupId", testGroupId)).toFuture())
      stored.size              shouldBe 1
      stored.head.status       shouldBe Active
      stored.head.formBundleId shouldBe None
    }
  }
}
