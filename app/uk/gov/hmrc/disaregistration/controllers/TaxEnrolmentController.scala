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
import uk.gov.hmrc.disaregistration.service.TaxEnrolmentService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject

class TaxEnrolmentController @Inject() (
                                         cc: ControllerComponents,
                                         service: TaxEnrolmentService
                                       )
  extends BackendController(cc) with Logging {

  def callback(): Action[AnyContent] = Action { implicit request =>
    request.body.asJson.fold {
        val msg = "Received tax enrolment callback with empty or non-JSON body"
        logger.warn(msg)
        BadRequest(msg)
    }{ js =>
        js.validate[TaxEnrolmentCallback].fold(
          errors => {
            logger.warn(s"Received invalid tax enrolment callback payload: ${JsError.toJson(errors)}")
            BadRequest
          },
          payload => {
            service.handle(payload)
            NoContent
          }
        )
    }
  }
}
