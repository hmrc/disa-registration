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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.mongodb.scala.{ClientSession, SingleObservableFuture}
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentSubscriberRequest
import uk.gov.hmrc.disaregistration.service.SubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.{ExecutionContext, Future}

class SubmissionServiceSpec extends BaseUnitSpec {

  private val service = new SubmissionService(
    mockEtmpConnector,
    mockJourneyAnswersService,
    mockTaxEnrolmentsConnector,
    mockAppConfig,
    mockMongoComponent
  )

  implicit val session: ClientSession = await(mockMongoComponent.client.startSession().toFuture())

  private def successfulEtmpAndStore(): Unit = {
    when(mockEtmpConnector.declareAndSubmit(eqTo(testEtmpSubmission))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(EnrolmentSubmissionResponse(testFormBundleId))))
    when(
      mockJourneyAnswersService.storeSubscriptionIdAndMarkSubmitted(
        eqTo(testJourneyData.groupId),
        eqTo(testFormBundleId)
      )(any[ExecutionContext], any[ClientSession])
    ).thenReturn(Future.successful(testFormBundleId))
    when(mockAppConfig.taxEnrolmentsServiceName).thenReturn("HMRC-DISA-ORG")
    when(mockAppConfig.taxEnrolmentsCallbackUrl(testFormBundleId)).thenReturn("callback-url")
    when(
      mockTaxEnrolmentsConnector.subscribe(eqTo(testFormBundleId), any[TaxEnrolmentSubscriberRequest])(
        any[HeaderCarrier]
      )
    ).thenReturn(Future.successful(Right(HttpResponse(204))))
  }

  "SubmissionService.declareAndSubmit" should {
    "store the form bundle and subscribe inline" in {
      successfulEtmpAndStore()

      service.declareAndSubmit(testJourneyData).futureValue shouldBe testFormBundleId

      verify(mockTaxEnrolmentsConnector).subscribe(
        eqTo(testFormBundleId),
        eqTo(TaxEnrolmentSubscriberRequest("HMRC-DISA-ORG", "callback-url", testString))
      )(any[HeaderCarrier])
    }

    "return the form bundle when the tax enrolment subscription fails" in {
      successfulEtmpAndStore()
      val error = UpstreamErrorResponse("bad", 500, 500, Map.empty)
      when(
        mockTaxEnrolmentsConnector.subscribe(any[String], any[TaxEnrolmentSubscriberRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Left(error)))

      service.declareAndSubmit(testJourneyData).futureValue shouldBe testFormBundleId
    }

    "return the form bundle when the tax enrolment connector fails" in {
      successfulEtmpAndStore()
      when(
        mockTaxEnrolmentsConnector.subscribe(any[String], any[TaxEnrolmentSubscriberRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.failed(new RuntimeException("connection failed")))

      service.declareAndSubmit(testJourneyData).futureValue shouldBe testFormBundleId
    }

    "fail when ETMP submission fails without subscribing" in {
      val error = UpstreamErrorResponse("bad", 500, 500, Map.empty)
      when(mockEtmpConnector.declareAndSubmit(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(error)))

      service.declareAndSubmit(testJourneyData).failed.futureValue shouldBe error
      verifyNoInteractions(mockTaxEnrolmentsConnector)
    }
  }
}
