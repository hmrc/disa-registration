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

package uk.gov.hmrc.disaregistration.service

import play.api.Logging
import uk.gov.hmrc.disaregistration.connectors.EtmpConnector
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.etmpsubmission.EtmpSubmission
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubmissionService @Inject() (
  etmpConnector: EtmpConnector,
  journeyAnswersService: JourneyAnswersService,
  taxEnrolmentService: TaxEnrolmentService
)(implicit ec: ExecutionContext)
    extends Logging {

  def declareAndSubmit(enrolment: JourneyData)(implicit hc: HeaderCarrier): Future[String] =
    EtmpSubmission(enrolment) match {

      case Left(error) =>
        logger.error(s"[SubmissionService] Submission validation failed: $error")
        Future.failed(new IllegalArgumentException(error))

      case Right(submission) =>
        etmpConnector.declareAndSubmit(submission).flatMap {
          case Left(upstreamError) =>
            Future.failed(upstreamError)

          case Right(EnrolmentSubmissionResponse(formBundleId)) =>
            journeyAnswersService
              .storeSubscriptionIdAndMarkSubmitted(
                groupId = enrolment.groupId,
                formBundleId = formBundleId
              )
              .flatMap(storedSubscriptionId => subscribeToTaxEnrolments(enrolment, storedSubscriptionId))
        }
    }

  private def subscribeToTaxEnrolments(enrolment: JourneyData, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    enrolment.businessVerification.flatMap(_.businessPartnerId) match {
      case Some(bpSafeId) =>
        taxEnrolmentService
          .subscribe(formBundleId, bpSafeId)
          .recover { case NonFatal(e) =>
            logger.error(
              s"Tax Enrolments subscription failed for formBundleId [$formBundleId] and bpSafeId [$bpSafeId]",
              e
            )
          }
          .map(_ => formBundleId)
      case None           =>
        logger.error(
          s"Tax Enrolments subscription failed for formBundleId [$formBundleId]: missing bpSafeId/businessPartnerId"
        )
        Future.successful(formBundleId)
    }
}
