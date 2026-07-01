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
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentWorkItem
import uk.gov.hmrc.disaregistration.service.SubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.{ExecutionContext, Future}

class SubmissionServiceSpec extends BaseUnitSpec {

  private val service =
    new SubmissionService(mockEtmpConnector, mockJourneyAnswersService, mockSubscribeTaxEnrolmentWorkItemRepository)

  "SubmissionService.declareAndSubmit" should {

    "returns formBundleId, stores formBundleId and subscribes to Tax Enrolments when ETMP submission succeeds" in {
      val testWorkItemPayload = TaxEnrolmentWorkItem(testFormBundleId, "bpSafeId")
      when(mockEtmpConnector.declareAndSubmit(eqTo(testEtmpSubmission))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EnrolmentSubmissionResponse(testFormBundleId))))

      when(
        mockJourneyAnswersService
          .storeSubscriptionIdAndMarkSubmitted(eqTo(testJourneyData.groupId), eqTo(testFormBundleId))(
            any[ExecutionContext]
          )
      )
        .thenReturn(Future.successful(testFormBundleId))

      when(mockSubscribeTaxEnrolmentWorkItemRepository.enqueue(any(), any()))
        .thenReturn(Future.successful(dummyWorkItem(testWorkItemPayload)))

      val result = service.declareAndSubmit(testJourneyData).futureValue

      result mustEqual testFormBundleId
      verify(mockSubscribeTaxEnrolmentWorkItemRepository).enqueue(eqTo(testFormBundleId), eqTo(testString))
    }

    "fails when ETMP returns Left(UpstreamErrorResponse)" in {
      val upstreamErrorResponse: UpstreamErrorResponse = UpstreamErrorResponse(
        message = "Internal Server Error",
        statusCode = 500,
        reportAs = 500,
        headers = Map.empty
      )

      when(mockEtmpConnector.declareAndSubmit(eqTo(testEtmpSubmission))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(upstreamErrorResponse)))

      val thrown = service.declareAndSubmit(testJourneyData).failed.futureValue

      thrown mustBe upstreamErrorResponse
      verifyNoInteractions(mockTaxEnrolmentService)
    }

    "fails when storing formBundleId fails after successful ETMP submission" in {
      val ex = new RuntimeException("mongo down")

      when(mockEtmpConnector.declareAndSubmit(eqTo(testEtmpSubmission))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EnrolmentSubmissionResponse(testFormBundleId))))

      when(
        mockJourneyAnswersService
          .storeSubscriptionIdAndMarkSubmitted(eqTo(testJourneyData.groupId), eqTo(testFormBundleId))(
            any[ExecutionContext]
          )
      )
        .thenReturn(Future.failed(ex))

      val thrown = service.declareAndSubmit(testJourneyData).failed.futureValue

      thrown mustBe ex
      verifyNoInteractions(mockTaxEnrolmentService)
    }

    "fails when enqueuing the Tax Enrolments work item fails after successful ETMP submission" in {
      val ex = new RuntimeException("mongo down")

      when(mockEtmpConnector.declareAndSubmit(eqTo(testEtmpSubmission))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EnrolmentSubmissionResponse(testFormBundleId))))

      when(
        mockJourneyAnswersService
          .storeSubscriptionIdAndMarkSubmitted(eqTo(testJourneyData.groupId), eqTo(testFormBundleId))(
            any[ExecutionContext]
          )
      )
        .thenReturn(Future.successful(testFormBundleId))

      when(mockSubscribeTaxEnrolmentWorkItemRepository.enqueue(eqTo(testFormBundleId), eqTo(testString)))
        .thenReturn(Future.failed(ex))

      val thrown = service.declareAndSubmit(testJourneyData).failed.futureValue

      thrown mustBe ex
    }

    "returns formBundleId and does not subscribe when bpSafeId is missing" in {
      val journeyDataWithoutBpSafeId: JourneyData = testJourneyData.copy(
        businessVerification = testJourneyData.businessVerification.map(_.copy(businessPartnerId = None))
      )

      when(mockEtmpConnector.declareAndSubmit(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(EnrolmentSubmissionResponse(testFormBundleId))))

      when(
        mockJourneyAnswersService
          .storeSubscriptionIdAndMarkSubmitted(eqTo(journeyDataWithoutBpSafeId.groupId), eqTo(testFormBundleId))(
            any[ExecutionContext]
          )
      )
        .thenReturn(Future.successful(testFormBundleId))

      val result = service.declareAndSubmit(journeyDataWithoutBpSafeId).futureValue

      result mustEqual testFormBundleId
      verifyNoInteractions(mockTaxEnrolmentService)
    }
  }
}
