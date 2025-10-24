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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.disaregistration.controllers.JourneyAnswersController
import utils.BaseUnitSpec

import scala.concurrent.Future

class JourneyAnswersControllerSpec extends BaseUnitSpec {

  val controller: JourneyAnswersController = app.injector.instanceOf[JourneyAnswersController]
  val registrationJson: JsValue            = Json.toJson(journeyData)

  def authorisedUser(): Unit =
    when(mockAuthConnector.authorise(any, any[Retrieval[Unit]])(any, any)).thenReturn(Future.successful(()))

  "JourneyAnswersController.retrieve" should {
    "return 200 OK when journeyData exists" in {
      authorisedUser()
      when(mockJourneyAnswersService.retrieve(groupId)).thenReturn(Future.successful(Some(journeyData)))

      val result = controller.retrieve(groupId)(FakeRequest())

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe registrationJson
    }

    "return 404 Not Found when no journeyData is found" in {
      authorisedUser()
      when(mockJourneyAnswersService.retrieve(groupId)).thenReturn(Future.successful(None))

      val result = controller.retrieve(groupId)(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }
  }

  "JourneyAnswersController.store" should {
    "return 200 OK when journeyData is stored successful" in {
      authorisedUser()
      when(mockJourneyAnswersService.store(groupId, journeyData)).thenReturn(Future.successful(journeyData))

      val request = FakeRequest()
        .withBody(registrationJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(groupId)(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe registrationJson
    }

    "return 500 Internal Server Error" in {
      authorisedUser()
      when(mockJourneyAnswersService.store(groupId, journeyData))
        .thenReturn(Future.failed(new RuntimeException("DB error")))

      val request = FakeRequest()
        .withBody(registrationJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(groupId)(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("There has been an issue processing your request")
    }

  }

}
