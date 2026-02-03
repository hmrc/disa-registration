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

package uk.gov.hmrc.disaregistration.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.service.{EtmpService, JourneyAnswersService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionController @Inject() (
  cc: ControllerComponents,
  journeyAnswersService: JourneyAnswersService,
  val authConnector: AuthConnector,
  etmpService: EtmpService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with WithJsonBody
    with Logging
    with AuthorisedFunctions {

  def declareAndSubmit(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    val journeyDataRetrieval = journeyAnswersService.retrieve(groupId)

    journeyDataRetrieval
      .flatMap {
        case Some(jd) =>
          etmpService.declareAndSubmit(jd).map { receiptId =>
            logger.info(s"Enrolment submission successful for IM: [$groupId] with receipt: [$receiptId]")
            Ok(Json.toJson(EnrolmentSubmissionResponse(receiptId)))
          }
        case None     =>
          logger.error(s"Failed to find journey data to submit for groupId [$groupId]")
          Future.successful(NotFound("Failed to find journey data to submit for this request"))
      }
      .recover { case e =>
        logger.error(s"Enrolment submission failed unexpectedly for [$groupId] with error: [$e]")
        InternalServerError("There has been an issue processing your request")
      }
  }

}
