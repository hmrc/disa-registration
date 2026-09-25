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
import play.api.libs.json.JsError
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentCallback
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentCallbackState._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.Future

class TaxEnrolmentController @Inject() (
  cc: ControllerComponents
) extends BackendController(cc)
    with Logging {

  def callback(formBundleId: String): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.fold(
      Future.successful {
        val msg = "Received tax enrolment callback with empty or non-JSON body"
        logger.warn(msg)
        BadRequest(msg)
      }
    ) { js =>
      js.validate[TaxEnrolmentCallback]
        .fold(
          errors => {
            logger.warn(s"Received invalid tax enrolment callback payload: ${JsError.toJson(errors)}")
            Future.successful(BadRequest)
          },
          payload => {
            payload.state match {
              case Succeeded      =>
                logger.info(
                  s"Received Tax Enrolments subscription callback with state [SUCCEEDED] for url [${payload.url}]"
                )
              case Enrolled       =>
                logger.warn(
                  s"Received Tax Enrolments subscription callback with state [Enrolled] for url [${payload.url}]" +
                    s"and errorResponse [${payload.errorResponse.getOrElse("missing errorResponse")}]"
                )
              case AuthRefreshed  =>
                logger.warn(
                  s"Received Tax Enrolments subscription callback with state [AuthRefreshed] for url [${payload.url}]" +
                    s"and errorResponse [${payload.errorResponse.getOrElse("missing errorResponse")}]"
                )
              case Error          =>
                logger.error(
                  s"Received Tax Enrolments subscription callback with state [ERROR] for url [${payload.url}] " +
                    s"and errorResponse [${payload.errorResponse.getOrElse("missing errorResponse")}]"
                )
              case EnrolmentError =>
                logger.warn(
                  s"Received Tax Enrolments subscription callback with state [EnrolmentError] for url [${payload.url}] " +
                    s"and errorResponse [${payload.errorResponse.getOrElse("missing errorResponse")}]"
                )
            }
            Future.successful(NoContent)
          }
        )
    }
  }
}
