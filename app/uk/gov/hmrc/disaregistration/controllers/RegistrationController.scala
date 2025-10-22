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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.disaregistration.models.Registration
import uk.gov.hmrc.disaregistration.service.RegistrationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RegistrationController @Inject() (
  cc: ControllerComponents,
  registrationService: RegistrationService,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with WithJsonBody
    with Logging
    with AuthorisedFunctions {

  def retrieve(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      registrationService.retrieve(groupId).map {
        case Some(registration) => Ok(Json.toJson(registration))
        case None               => NotFound(s"Registration not found for groupId: $groupId")
      }
    }
  }

  def store(groupId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    authorised() {
      withJsonBody[Registration] { registration =>
        registrationService
          .store(groupId, registration)
          .map(registration => Ok(Json.toJson(registration)))
          .recover { case ex =>
            logger.error(s"[RegistrationController][store] Failed to store registration for groupId: $groupId", ex)
            InternalServerError("There has been an issue processing your request")
          }
      }
    }
  }
}
