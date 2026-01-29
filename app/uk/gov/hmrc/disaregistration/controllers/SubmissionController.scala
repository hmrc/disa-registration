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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
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

    val submissionResult = journeyDataRetrieval.flatMap {
      case Some(jd) => etmpService.declareAndSubmit(jd)
      case None     => Future(Left("sadness")) //TODO something sensible
    }

    submissionResult.map {
      case Left(error)         =>
        logger.error(s"Failed to declare and submit for IM: [$groupId] with error: [$error]")
        InternalServerError
      case Right(submissionId) =>
        logger.info(s"Enrolment submission successful for IM: [$groupId]")
        Ok(submissionId)
    }
  }

}
