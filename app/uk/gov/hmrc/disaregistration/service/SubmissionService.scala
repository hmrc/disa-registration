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
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.connectors.{EtmpConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.etmpsubmission.EtmpSubmission
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentSubscriberRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubmissionService @Inject() (
  etmpConnector: EtmpConnector,
  journeyAnswersService: JourneyAnswersService,
  taxEnrolmentsConnector: TaxEnrolmentsConnector,
  appConfig: AppConfig,
  val mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends Logging
    with Transactions {
  private implicit val tc: TransactionConfiguration = TransactionConfiguration.strict

  def declareAndSubmit(enrolment: JourneyData)(implicit hc: HeaderCarrier): Future[String] =
    EtmpSubmission(enrolment) match {

      case Left(error) =>
        logger.error(s"[SubmissionService][declareAndSubmit] Submission validation failed: $error")
        Future.failed(new IllegalArgumentException(error))

      case Right(submission) =>
        enrolment.businessVerification.flatMap(_.businessPartnerId) match {
          case Some(bpSafeId) =>
            etmpConnector.declareAndSubmit(submission).flatMap {
              case Left(upstreamError) =>
                Future.failed(upstreamError)

              case Right(EnrolmentSubmissionResponse(formBundleId)) =>
                withSessionAndTransaction[String] { implicit session =>
                  journeyAnswersService.storeSubscriptionIdAndMarkSubmitted(
                    groupId = enrolment.groupId,
                    formBundleId = formBundleId
                  )
                }.flatMap { storedFormBundleId =>
                  subscribeToTaxEnrolments(storedFormBundleId, bpSafeId).map(_ => storedFormBundleId)
                }
            }

          case None =>
            val ex = new IllegalStateException(
              "Missing businessPartnerId from businessVerification"
            )
            logger.error(s"[SubmissionService][declareAndSubmit] ${ex.getMessage}")
            Future.failed(ex)
        }
    }

  private def subscribeToTaxEnrolments(formBundleId: String, etmpId: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    val request = TaxEnrolmentSubscriberRequest(
      serviceName = appConfig.taxEnrolmentsServiceName,
      callback = appConfig.taxEnrolmentsCallbackUrl(formBundleId),
      etmpId = etmpId
    )

    taxEnrolmentsConnector
      .subscribe(formBundleId, request)
      .map {
        case Right(_)    =>
          logger.info(
            s"[SubmissionService][subscribeToTaxEnrolments] Tax Enrolments subscription request successful for formBundleId [$formBundleId] and etmpId [$etmpId]"
          )
          ()
        case Left(error) =>
          logger.error(
            s"[SubmissionService][subscribeToTaxEnrolments] Tax Enrolments subscription request failed for formBundleId [$formBundleId] and etmpId [$etmpId] " +
              s"with status [${error.statusCode}] and message [${error.message}]"
          )
          ()
      }
      .recover { case NonFatal(error) =>
        logger.error(
          s"[SubmissionService][subscribeToTaxEnrolments] Tax Enrolments subscription request failed for formBundleId [$formBundleId] and etmpId [$etmpId]",
          error
        )
        ()
      }
  }
}
