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

package uk.gov.hmrc.disaregistration.models.journeyData.orgdetails

import play.api.libs.json._

sealed trait SelectedCorrespondenceAddress

object SelectedCorrespondenceAddress {
  final case class LookupAddress(index: Int) extends SelectedCorrespondenceAddress

  case object ManualEntry extends SelectedCorrespondenceAddress

  private val LookupType = "lookup"
  private val ManualType = "manual"

  implicit val format: Format[SelectedCorrespondenceAddress] =
    Format(
      Reads {
        case json if (json \ "type").asOpt[String].contains(LookupType) =>
          (json \ "index").validate[Int].map(LookupAddress.apply)
        case json if (json \ "type").asOpt[String].contains(ManualType) =>
          JsSuccess(ManualEntry)
        case _                                                          =>
          JsError("Invalid SelectedCorrespondenceAddress")
      },
      Writes {
        case LookupAddress(index) =>
          Json.obj(
            "type"  -> LookupType,
            "index" -> index
          )

        case ManualEntry =>
          Json.obj(
            "type" -> ManualType
          )
      }
    )
}
