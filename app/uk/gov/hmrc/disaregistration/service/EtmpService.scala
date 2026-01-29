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
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.disaregistration.connectors.EtmpConnector
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EtmpService @Inject() (etmpConnector: EtmpConnector)(implicit ec: ExecutionContext) extends Logging {
  def declareAndSubmit(enrolmentSubmission: JourneyData)(implicit hc: HeaderCarrier): Future[Either[Result, String]] =
    etmpConnector.declareAndSubmit(enrolmentSubmission).map {
      case Left(upstreamError) =>
        logger.error(
          s"ETMP call failed for groupId [${enrolmentSubmission.groupId}] " +
            s"status=[${upstreamError.statusCode}] message=[${upstreamError.message}]"
        )
        Left(InternalServerError)
      case Right(submissionId)    => Right(submissionId)
    }

}
