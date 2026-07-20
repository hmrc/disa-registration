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

package service

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentSubscriberRequest
import uk.gov.hmrc.disaregistration.service.TaxEnrolmentService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class TaxEnrolmentServiceSpec extends BaseUnitSpec {

  private val serviceName = "HMRC-DISA-ORG"
  private val callbackUrl = s"http://localhost:1203/disa-registration/callback/subscriptions/$testFormBundleId"
  private val service     = new TaxEnrolmentService(mockTaxEnrolmentsConnector, mockAppConfig)

  "TaxEnrolmentService.subscribe" should {

    "send the configured subscriber request and return Right when Tax Enrolments succeeds" in {
      val request = TaxEnrolmentSubscriberRequest(serviceName, callbackUrl, testString)

      when(mockAppConfig.taxEnrolmentsServiceName).thenReturn(serviceName)
      when(mockAppConfig.taxEnrolmentsCallbackUrl(eqTo(testFormBundleId))).thenReturn(callbackUrl)
      when(mockTaxEnrolmentsConnector.subscribe(eqTo(testFormBundleId), eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(NO_CONTENT, ""))))

      val result = service.subscribe(testFormBundleId, testString).futureValue

      result mustBe Right(())
    }

    "return Left when Tax Enrolments returns an upstream error" in {
      val request               = TaxEnrolmentSubscriberRequest(serviceName, callbackUrl, testString)
      val upstreamErrorResponse = UpstreamErrorResponse(
        message = "Bad request",
        statusCode = BAD_REQUEST,
        reportAs = BAD_REQUEST,
        headers = Map.empty
      )

      when(mockAppConfig.taxEnrolmentsServiceName).thenReturn(serviceName)
      when(mockAppConfig.taxEnrolmentsCallbackUrl(eqTo(testFormBundleId))).thenReturn(callbackUrl)
      when(mockTaxEnrolmentsConnector.subscribe(eqTo(testFormBundleId), eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(upstreamErrorResponse)))

      val result = service.subscribe(testFormBundleId, testString).futureValue

      result mustBe Left(upstreamErrorResponse)
    }

    "fail when the Tax Enrolments connector fails unexpectedly" in {
      val exception = new RuntimeException("connection failed")
      val request   = TaxEnrolmentSubscriberRequest(serviceName, callbackUrl, testString)

      when(mockAppConfig.taxEnrolmentsServiceName).thenReturn(serviceName)
      when(mockAppConfig.taxEnrolmentsCallbackUrl(eqTo(testFormBundleId))).thenReturn(callbackUrl)
      when(mockTaxEnrolmentsConnector.subscribe(eqTo(testFormBundleId), eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(exception))

      val result = service.subscribe(testFormBundleId, testString).failed.futureValue

      result mustBe exception
    }
  }
}
