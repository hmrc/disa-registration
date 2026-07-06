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
import uk.gov.hmrc.disaregistration.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentCallback
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentCallbackState._
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentSubscriberRequest
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class TaxEnrolmentService @Inject() (
  taxEnrolmentsConnector: TaxEnrolmentsConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def subscribe(formBundleId: String, etmpId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val request = TaxEnrolmentSubscriberRequest(
      serviceName = appConfig.taxEnrolmentsServiceName,
      callback = appConfig.taxEnrolmentsCallbackUrl(formBundleId),
      etmpId = etmpId
    )

    taxEnrolmentsConnector
      .subscribe(formBundleId, request)
      .map {
        case Right(_)            =>
          logger.info(
            s"Tax Enrolments subscription request successful for formBundleId [$formBundleId] and etmpId [$etmpId]"
          )
        case Left(upstreamError) =>
          logger.error(
            s"Tax Enrolments subscription request failed for formBundleId [$formBundleId] and etmpId [$etmpId] " +
              s"with status [${upstreamError.statusCode}] and message [${upstreamError.message}]"
          )
      }
      .recover { case NonFatal(e) =>
        logger.error(
          s"Tax Enrolments subscription request failed unexpectedly for formBundleId [$formBundleId] and etmpId [$etmpId]",
          e
        )
      }
  }

  def handle(callback: TaxEnrolmentCallback): Future[Unit] =
    Future(callback.state match {
      case Succeeded =>
        logger.info(
          s"Received Tax Enrolments subscription callback with state [SUCCEEDED] for url [${callback.url}]"
        )

      case Enrolled =>
        logger.warn(
          s"Received Tax Enrolments subscription callback with state [Enrolled] for url [${callback.url}]" +
            s"and errorResponse [${callback.errorResponse.getOrElse("missing errorResponse")}]"
        )

      case AuthRefreshed =>
        logger.warn(
          s"Received Tax Enrolments subscription callback with state [AuthRefreshed] for url [${callback.url}]" +
            s"and errorResponse [${callback.errorResponse.getOrElse("missing errorResponse")}]"
        )

      case Error =>
        logger.error(
          s"Received Tax Enrolments subscription callback with state [ERROR] for url [${callback.url}] " +
            s"and errorResponse [${callback.errorResponse.getOrElse("missing errorResponse")}]"
        )

      case EnrolmentError =>
        logger.warn(
          s"Received Tax Enrolments subscription callback with state [EnrolmentError] for url [${callback.url}] " +
            s"and errorResponse [${callback.errorResponse.getOrElse("missing errorResponse")}]"
        )
    })
}
