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

package uk.gov.hmrc.disaregistration.models.taxenrolments

import play.api.libs.json._

sealed trait TaxEnrolmentCallbackState {
  val value: String
}

object TaxEnrolmentCallbackState {

  case object Error          extends TaxEnrolmentCallbackState { val value = "ERROR" }
  case object Succeeded      extends TaxEnrolmentCallbackState { val value = "SUCCEEDED" }
  case object EnrolmentError extends TaxEnrolmentCallbackState { val value = "EnrolmentError" }
  case object Enrolled       extends TaxEnrolmentCallbackState { val value = "Enrolled" }
  case object AuthRefreshed  extends TaxEnrolmentCallbackState { val value = "AuthRefreshed" }

  val values: Seq[TaxEnrolmentCallbackState] =
    Seq(Error, Succeeded, EnrolmentError, Enrolled, AuthRefreshed)

  def fromString(value: String): Option[TaxEnrolmentCallbackState] =
    values.find(_.value == value)

  implicit val reads: Reads[TaxEnrolmentCallbackState] =
    Reads {
      case JsString(value) =>
        fromString(value)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Invalid enrolment callback state: $value"))
      case _               =>
        JsError("State must be a string")
    }

  implicit val writes: Writes[TaxEnrolmentCallbackState] =
    Writes(state => JsString(state.value))

  implicit val format: Format[TaxEnrolmentCallbackState] = Format(reads, writes)
}