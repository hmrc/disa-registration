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

package controllers

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.disaregistration.controllers.JourneyAnswersController
import uk.gov.hmrc.disaregistration.models.GetOrCreateEnrolmentResult
import utils.BaseUnitSpec

import scala.concurrent.Future

class JourneyAnswersControllerSpec extends BaseUnitSpec {

  val controller: JourneyAnswersController = app.injector.instanceOf[JourneyAnswersController]

  val organisationDetailsJson: JsValue = Json.toJson(organisationDetails)

  val taskListJourney = "organisationDetails"

  def authorisedUser(): Unit =
    when(mockAuthConnector.authorise(any, any[Retrieval[Unit]])(any, any))
      .thenReturn(Future.successful(()))

  "JourneyAnswersController.retrieve" should {

    "return 200 OK when journeyData exists" in {
      authorisedUser()
      when(mockJourneyAnswersService.retrieve(testGroupId))
        .thenReturn(Future.successful(Some(testJourneyData)))

      val result = controller.retrieve(testGroupId)(FakeRequest())

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(testJourneyData)
    }

    "return 404 Not Found when no journeyData is found" in {
      authorisedUser()
      when(mockJourneyAnswersService.retrieve(testGroupId)).thenReturn(Future.successful(None))

      val result = controller.retrieve(testGroupId)(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }
  }

  "JourneyAnswersController.getOrCreateEnrolment" should {

    "return 201 Created when a new enrolment journey is created" in {
      authorisedUser()

      val serviceResult = GetOrCreateEnrolmentResult(
        isNewEnrolment = true,
        journeyData = testJourneyData
      )

      when(mockJourneyAnswersService.getOrCreateEnrolment(ArgumentMatchers.eq(testGroupId)))
        .thenReturn(Future.successful(serviceResult))

      val result = controller.getOrCreateEnrolment(testGroupId)(FakeRequest())

      status(result)        shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(serviceResult)
    }

    "return 200 OK when an existing enrolment journey is found" in {
      authorisedUser()

      val serviceResult = GetOrCreateEnrolmentResult(
        isNewEnrolment = false,
        journeyData = testJourneyData
      )

      when(mockJourneyAnswersService.getOrCreateEnrolment(ArgumentMatchers.eq(testGroupId)))
        .thenReturn(Future.successful(serviceResult))

      val result = controller.getOrCreateEnrolment(testGroupId)(FakeRequest())

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(serviceResult)
    }

    "return 500 InternalServerError when the service throws an exception" in {
      authorisedUser()

      when(mockJourneyAnswersService.getOrCreateEnrolment(ArgumentMatchers.eq(testGroupId)))
        .thenReturn(Future.failed(new RuntimeException("fubar")))

      val result = controller.getOrCreateEnrolment(testGroupId)(FakeRequest())

      status(result)          shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe "There has been an issue processing your request"
    }
  }

  "JourneyAnswersController.store" should {

    "return 204 NoContent when the journey section is successfully updated" in {
      authorisedUser()

      when(
        mockJourneyAnswersService.storeJourneyData(
          ArgumentMatchers.eq(testGroupId),
          ArgumentMatchers.eq(taskListJourney),
          ArgumentMatchers.eq(organisationDetails)
        )(any)
      ).thenReturn(Future.successful(()))

      val request = FakeRequest()
        .withBody(organisationDetailsJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(testGroupId, taskListJourney)(request)

      status(result) shouldBe NO_CONTENT
    }

    "return 400 BadRequest when the taskListJourney is invalid" in {
      authorisedUser()

      val invalidJourney = "invalidSection"

      val request = FakeRequest()
        .withBody(organisationDetailsJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(testGroupId, invalidJourney)(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Invalid taskListJourney parameter")
    }

    "return 400 BadRequest when the JSON does not validate for the journey section" in {
      authorisedUser()

      val invalidJson = Json.obj(
        "registeredToManageIsa" -> "not-a-boolean"
      )

      val request = FakeRequest()
        .withBody(invalidJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(testGroupId, taskListJourney)(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Invalid JSON for taskListJourney")
    }

    "return 500 InternalServerError when the service throws an unexpected exception" in {
      authorisedUser()

      when(
        mockJourneyAnswersService.storeJourneyData(
          ArgumentMatchers.eq(testGroupId),
          ArgumentMatchers.eq(taskListJourney),
          ArgumentMatchers.eq(organisationDetails)
        )(any)
      ).thenReturn(Future.failed(new RuntimeException("DB exploded")))

      val request = FakeRequest()
        .withBody(organisationDetailsJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(testGroupId, taskListJourney)(request)

      status(result)          shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe "There has been an issue processing your request"
    }
  }
}
