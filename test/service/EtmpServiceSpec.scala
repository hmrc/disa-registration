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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.service.EtmpService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.{ExecutionContext, Future}

class EtmpServiceSpec extends BaseUnitSpec {

  private val service = new EtmpService(mockEtmpConnector, mockJourneyAnswersService)

  "EtmpService.declareAndSubmit" should {

    "returns receiptId and stores receipt when ETMP submission succeeds" in {
      when(mockEtmpConnector.declareAndSubmit(eqTo(testJourneyData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EnrolmentSubmissionResponse(testReceiptId))))

      when(
        mockJourneyAnswersService.storeReceiptAndMarkSubmitted(eqTo(testJourneyData.groupId), eqTo(testReceiptId))(
          any[ExecutionContext]
        )
      )
        .thenReturn(Future.successful(testReceiptId))

      val result = service.declareAndSubmit(testJourneyData).futureValue

      result mustEqual testReceiptId
    }

    "fails when ETMP returns Left(UpstreamErrorResponse)" in {
      val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
        message = "Internal Server Error",
        statusCode = 500,
        reportAs = 500,
        headers = Map.empty
      )

      when(mockEtmpConnector.declareAndSubmit(eqTo(testJourneyData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(upstreamErrorResponse)))

      val thrown = service.declareAndSubmit(testJourneyData).failed.futureValue

      thrown mustBe upstreamErrorResponse
    }

    "fails when storing receipt fails after successful ETMP submission" in {
      val ex = new RuntimeException("mongo down")

      when(mockEtmpConnector.declareAndSubmit(eqTo(testJourneyData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EnrolmentSubmissionResponse(testReceiptId))))

      when(
        mockJourneyAnswersService.storeReceiptAndMarkSubmitted(eqTo(testJourneyData.groupId), eqTo(testReceiptId))(
          any[ExecutionContext]
        )
      )
        .thenReturn(Future.failed(ex))

      val thrown = service.declareAndSubmit(testJourneyData).failed.futureValue

      thrown mustBe ex
    }
  }
}
