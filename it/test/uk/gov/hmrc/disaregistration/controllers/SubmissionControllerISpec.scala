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

import org.mongodb.scala.model.Filters
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import play.api.{Application, inject}
import uk.gov.hmrc.disaregistration.jobs.SubscriptionEnrolmentWorkItemJob
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.journeyData.EnrolmentStatus.{Active, Submitted}
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.disaregistration.repositories.{JourneyAnswersRepository, SubscribeTaxEnrollmentWorkItemRepository}
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaregistration.utils.WiremockHelper.stubPost
import uk.gov.hmrc.mongo.MongoComponent

class SubmissionControllerISpec extends BaseIntegrationSpec with MockitoSugar {

  private val databaseName: String                    = "disa-registration-submission-test"
  private lazy val mongoUri: String                   = s"mongodb://127.0.0.1:27017/$databaseName"
  private lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  override lazy val app: Application = app(
    inject.bind[MongoComponent].toInstance(mockMongoComponent),
    inject.bind[SubscriptionEnrolmentWorkItemJob].toInstance(mock[SubscriptionEnrolmentWorkItemJob])
  )
  val repo: JourneyAnswersRepository                                 = app.injector.instanceOf[JourneyAnswersRepository]
  val workItemRepo: SubscribeTaxEnrollmentWorkItemRepository         =
    app.injector.instanceOf[SubscribeTaxEnrollmentWorkItemRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.drop().toFuture())
    await(workItemRepo.collection.drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repo.collection.drop().toFuture())
    await(workItemRepo.collection.drop().toFuture())
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

      val workItems = await(workItemRepo.collection.find().toFuture())
      workItems.size                   shouldBe 1
      workItems.head.item.formBundleId shouldBe testFormBundleId
      workItems.head.item.bpSafeId     shouldBe testString
    }

    "return 200 and enqueue Tax Enrolments work item" in {
      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testEnrolmentId,
        formBundleId = None,
        status = Active,
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

      response.status shouldBe OK
      response.json   shouldBe Json.toJson(EnrolmentSubmissionResponse(testFormBundleId))

      val stored = await(repo.collection.find(Filters.eq("groupId", testGroupId)).toFuture())
      stored.size              shouldBe 1
      stored.head.status       shouldBe Submitted
      stored.head.formBundleId shouldBe Some(testFormBundleId)

      val workItems = await(workItemRepo.collection.find().toFuture())
      workItems.size                   shouldBe 1
      workItems.head.item.formBundleId shouldBe testFormBundleId
      workItems.head.item.bpSafeId     shouldBe testString
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

      response.status shouldBe NOT_FOUND
      response.body   shouldBe "Failed to find journey data to submit for this request"
    }

    "return 500 when journey exists but no Active journey is available to submit" in {
      val jd = JourneyData(
        groupId = testGroupId,
        enrolmentId = testString,
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

      response.status shouldBe INTERNAL_SERVER_ERROR
      response.body   shouldBe "There has been an issue processing your request"

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

      response.status shouldBe INTERNAL_SERVER_ERROR
      response.body   shouldBe "There has been an issue processing your request"

      val stored = await(repo.collection.find(Filters.eq("groupId", testGroupId)).toFuture())
      stored.size              shouldBe 1
      stored.head.status       shouldBe Active
      stored.head.formBundleId shouldBe None
    }
  }
}
