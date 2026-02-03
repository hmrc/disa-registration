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

package controllers

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.disaregistration.controllers.routes
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.journeyData.EnrolmentStatus.Active
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.disaregistration.service.{EtmpService, JourneyAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseUnitSpec

import scala.concurrent.Future

class SubmissionControllerSpec extends BaseUnitSpec {

  private def application: Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[JourneyAnswersService].toInstance(mockJourneyAnswersService),
        bind[EtmpService].toInstance(mockEtmpService),
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()

  "SubmissionController.declareAndSubmit" should {

    "must return OK and a receipt json response when journey data exists and ETMP submission succeeds" in {

      val app             = application
      val jd: JourneyData =
        JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId, status = Active, receiptId = None)
      val receiptId       = testString

      when(mockJourneyAnswersService.retrieve(eqTo(testGroupId)))
        .thenReturn(Future.successful(Some(jd)))

      when(mockEtmpService.declareAndSubmit(eqTo(jd))(any[HeaderCarrier]))
        .thenReturn(Future.successful(receiptId))

      running(app) {
        val request = FakeRequest(POST, routes.SubmissionController.declareAndSubmit(testGroupId).url)

        val result = route(app, request).get

        status(result) mustEqual OK
        contentType(result) mustEqual Some("application/json")
        contentAsString(result) mustEqual Json.toJson(EnrolmentSubmissionResponse(receiptId)).toString
      }
    }

    "must return NotFound when journey data does not exist" in {

      val app = application

      when(mockJourneyAnswersService.retrieve(eqTo(testGroupId)))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = FakeRequest(POST, routes.SubmissionController.declareAndSubmit(testGroupId).url)

        val result = route(app, request).get

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustEqual "Failed to find journey data to submit for this request"
      }
    }

    "must return InternalServerError when journey retrieval fails" in {

      val app = application

      when(mockJourneyAnswersService.retrieve(eqTo(testGroupId)))
        .thenReturn(Future.failed(new RuntimeException("fubar")))

      running(app) {
        val request = FakeRequest(POST, routes.SubmissionController.declareAndSubmit(testGroupId).url)

        val result = route(app, request).get

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) mustEqual "There has been an issue processing your request"
      }
    }

    "must return InternalServerError when ETMP submission fails" in {

      val app             = application
      val jd: JourneyData =
        JourneyData(groupId = testGroupId, enrolmentId = testEnrolmentId, status = Active, receiptId = None)

      when(mockJourneyAnswersService.retrieve(eqTo(testGroupId)))
        .thenReturn(Future.successful(Some(jd)))

      when(mockEtmpService.declareAndSubmit(eqTo(jd))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("etmp down")))

      running(app) {
        val request = FakeRequest(POST, routes.SubmissionController.declareAndSubmit(testGroupId).url)

        val result = route(app, request).get

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) mustEqual "There has been an issue processing your request"
      }
    }
  }
}
