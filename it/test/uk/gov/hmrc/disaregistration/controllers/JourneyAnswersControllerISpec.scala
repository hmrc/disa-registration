/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec

class JourneyAnswersControllerISpec extends BaseIntegrationSpec {

  private lazy val journeyAnswersRepository = app.injector.instanceOf[JourneyAnswersRepository]

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(journeyAnswersRepository.collection.drop().toFuture())
  }

  val journeyDataJson: String =
    s"""{
      |  "id": "$testGroupId",
      |  "organisationDetails": {
      |    "registeredToManageIsa": false,
      |    "zRefNumber": "$testZRef",
      |    "fcaNumber": "6743765"
      |  },
      |  "lastUpdated": "2025-10-21T15:27:28.433131Z"
      |}""".stripMargin

  val organisationDetailsJson: String =
    s"""{
      |    "registeredToManageIsa": false,
      |    "zRefNumber": "$testZRef",
      |    "fcaNumber": "6743765"
      |}""".stripMargin

  val body: JsValue = Json.parse(organisationDetailsJson)

  "GET /store/:testGroupId" should {

    "return 404 Not Found when journeyData does not exist" in {
      retrieveJourneyAnswersRequest(groupId = testGroupId).status shouldBe NOT_FOUND
    }

    "return 200 OK when journeyData exists" in {
      storeJourneyAnswersRequest(taskListJourney = "organisationDetails", body = body).status shouldBe NO_CONTENT
      val result = retrieveJourneyAnswersRequest(groupId = testGroupId)

      result.status                                                               shouldBe OK
      (result.json \ "groupId").as[String]                                        shouldBe testGroupId
      (result.json \ "organisationDetails" \ "registeredToManageIsa").as[Boolean] shouldBe false
      (result.json \ "organisationDetails" \ "zRefNumber").as[String]             shouldBe testZRef
      (result.json \ "organisationDetails" \ "fcaNumber").as[String]              shouldBe "6743765"
    }

    "return 401 Unauthorized for an unauthorised request" in {
      stubAuthFail()
      val result = await(
        ws.url(s"http://localhost:$port/disa-registration/store/$testGroupId")
          .get()
      )

      result.status shouldBe UNAUTHORIZED
    }
  }

  "POST /store/:testGroupId/:taskListJourney" should {

    "return 204 NoContent when journeyData is successfully stored" in {
      storeJourneyAnswersRequest(taskListJourney = "organisationDetails", body = body).status shouldBe NO_CONTENT
    }

    "allow storing a second journey model into the same document and retrieving combined data" in {
      val orgDetailsResult =
        storeJourneyAnswersRequest(
          taskListJourney = "organisationDetails",
          body = body
        )

      orgDetailsResult.status shouldBe NO_CONTENT
      val firstRetrieve = retrieveJourneyAnswersRequest()

      firstRetrieve.status                                                   shouldBe OK
      (firstRetrieve.json \ "organisationDetails" \ "zRefNumber").as[String] shouldBe testZRef
      (firstRetrieve.json \ "businessVerification").toOption                 shouldBe None

      val businessVerificationJson = Json.obj(
        "dataItem"  -> "SomeValue",
        "dataItem2" -> "SomeOtherValue"
      )

      val verificationResult =
        storeJourneyAnswersRequest(
          taskListJourney = "businessVerification",
          body = businessVerificationJson
        )
      verificationResult.status shouldBe NO_CONTENT

      val secondRetrieve = retrieveJourneyAnswersRequest()
      secondRetrieve.status shouldBe OK

      (secondRetrieve.json \ "organisationDetails" \ "fcaNumber").as[String]  shouldBe "6743765"
      (secondRetrieve.json \ "businessVerification" \ "dataItem").as[String]  shouldBe "SomeValue"
      (secondRetrieve.json \ "businessVerification" \ "dataItem2").as[String] shouldBe "SomeOtherValue"
    }

    "return 400 BadRequest when taskListJourney is invalid" in {
      val result = storeJourneyAnswersRequest(
        taskListJourney = "nonExistentObject",
        body = body
      )

      result.status shouldBe BAD_REQUEST
      result.body     should include("Invalid taskListJourney parameter")
    }

    "return 400 BadRequest when JSON is invalid for a valid taskListJourney" in {
      val invalidJson = Json.obj(
        "registeredToManageIsa" -> "not-a-boolean"
      )

      val result = storeJourneyAnswersRequest(
        taskListJourney = "organisationDetails",
        body = invalidJson
      )

      result.status shouldBe BAD_REQUEST
      result.body     should include("Invalid JSON for taskListJourney")
    }
  }

  def storeJourneyAnswersRequest(
    groupId: String = testGroupId,
    taskListJourney: String,
    body: JsValue,
    headers: Seq[(String, String)] = testHeaders
  ): WSResponse = {
    stubAuth()
    await(
      ws.url(s"http://localhost:$port/disa-registration/store/$groupId/$taskListJourney")
        .withHttpHeaders(headers: _*)
        .post(body)
    )
  }

  def retrieveJourneyAnswersRequest(
    groupId: String = testGroupId,
    headers: Seq[(String, String)] = testHeaders
  ): WSResponse = {
    stubAuth()
    await(
      ws.url(s"http://localhost:$port/disa-registration/store/$groupId")
        .withHttpHeaders(headers: _*)
        .get()
    )
  }
}
