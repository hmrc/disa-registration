/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData.{TaskListJourney, taskListJourneyHandlers}
import uk.gov.hmrc.disaregistration.service.JourneyAnswersService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyAnswersController @Inject() (
  cc: ControllerComponents,
  journeyAnswersService: JourneyAnswersService,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with WithJsonBody
    with Logging
    with AuthorisedFunctions {

  def retrieve(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      journeyAnswersService.retrieve(groupId).map {
        case Some(registration) => Ok(Json.toJson(registration))
        case None               => NotFound
      }
    }
  }

  def store(groupId: String, taskListJourney: String): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      authorised() {
        taskListJourneyHandlers.get(taskListJourney) match {
          case None                                 =>
            logger.error(s"Invalid taskListJourney provided: $taskListJourney")
            Future.successful(BadRequest(s"Invalid taskListJourney parameter: '$taskListJourney'"))
          case Some(TaskListJourney(reads, writes)) =>
            request.body.validate(reads) match {
              case JsSuccess(model, _) =>
                journeyAnswersService
                  .storeJourneyData(groupId, taskListJourney, model)(writes)
                  .map(_ => NoContent)
              case JsError(errors)     =>
                logger.error(s"Invalid JSON for taskListJourney '$taskListJourney': ${JsError.toJson(errors)}")
                Future.successful(BadRequest(s"Invalid JSON for taskListJourney '$taskListJourney'"))
            }
        }
      }.recover { case e =>
        logger.error(s"Unexpected error storing $taskListJourney for $groupId", e)
        InternalServerError("There has been an issue processing your request")
      }
    }
}
