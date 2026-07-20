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

package uk.gov.hmrc.disaregistration.connectors

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentSubscriberRequest
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class TaxEnrolmentsConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    val connector: TaxEnrolmentsConnector = new TaxEnrolmentsConnector(mockHttpClient, mockAppConfig)

    val testUrl: String                        = "http://localhost:1203"
    val request: TaxEnrolmentSubscriberRequest =
      TaxEnrolmentSubscriberRequest("HMRC-DISA-ORG", "http://localhost/callback", testString)

    when(mockAppConfig.taxEnrolmentsBaseUrl).thenReturn(testUrl)

    when(mockHttpClient.put(url"$testUrl/tax-enrolments/subscriptions/$testFormBundleId/subscriber"))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any, any, any))
      .thenReturn(mockRequestBuilder)
  }

  "TaxEnrolmentsConnector.subscribe" should {

    "return Right(HttpResponse) when Tax Enrolments returns success" in new TestSetup {
      val response = HttpResponse(NO_CONTENT, "")

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(response)))

      val result: Either[UpstreamErrorResponse, HttpResponse] =
        connector.subscribe(testFormBundleId, request).futureValue

      result mustBe Right(response)
    }

    "return Left(UpstreamErrorResponse) when Tax Enrolments returns an error response" in new TestSetup {
      val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
        message = "Bad Request",
        statusCode = 400,
        reportAs = 400,
        headers = Map.empty
      )

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Left(upstreamErrorResponse)))

      val result: Either[UpstreamErrorResponse, HttpResponse] =
        connector.subscribe(testFormBundleId, request).futureValue

      result mustBe Left(upstreamErrorResponse)
    }

    "propagate Throwable when the call fails with an unexpected exception" in new TestSetup {
      val ex = new RuntimeException("Connection timeout")

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.failed(ex))

      val thrown: Throwable = connector.subscribe(testFormBundleId, request).failed.futureValue

      thrown mustBe ex
    }
  }
}
