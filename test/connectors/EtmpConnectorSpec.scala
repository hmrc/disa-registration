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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class EtmpConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    val connector: EtmpConnector = new EtmpConnector(mockHttpClient, mockAppConfig)

    val testUrl: String = "http://localhost:1201"
    when(mockAppConfig.etmpBaseUrl).thenReturn(testUrl)

    when(mockHttpClient.post(url"$testUrl/etmp/enrolment/submission"))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any, any, any))
      .thenReturn(mockRequestBuilder)
  }

  "EtmpConnector.declareAndSubmit" should {

    "return Right(EnrolmentSubmissionResponse) when the call succeeds" in new TestSetup {
      val submission: JourneyData = testJourneyData
      val response                = EnrolmentSubmissionResponse(testString)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, EnrolmentSubmissionResponse]](any(), any()))
        .thenReturn(Future.successful(Right(response)))

      val result = connector.declareAndSubmit(submission).futureValue

      result mustBe Right(response)
    }

    "return Left(UpstreamErrorResponse) when ETMP returns an UpstreamErrorResponse in the Either" in new TestSetup {
      val submission: JourneyData                      = testJourneyData
      val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
        message = "Not authorised",
        statusCode = 401,
        reportAs = 401,
        headers = Map.empty
      )

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, EnrolmentSubmissionResponse]](any(), any()))
        .thenReturn(Future.successful(Left(upstreamErrorResponse)))

      val result = connector.declareAndSubmit(submission).futureValue

      result mustBe Left(upstreamErrorResponse)
    }

    "propagate Throwable when the call fails with an unexpected exception" in new TestSetup {
      val submission: JourneyData = testJourneyData
      val ex                      = new RuntimeException("Connection timeout")

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, EnrolmentSubmissionResponse]](any(), any()))
        .thenReturn(Future.failed(ex))

      val thrown = connector.declareAndSubmit(submission).failed.futureValue

      thrown mustBe ex
    }
  }
}
