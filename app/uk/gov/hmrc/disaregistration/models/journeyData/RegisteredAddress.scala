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

package uk.gov.hmrc.disaregistration.models.journeyData

import play.api.libs.json.{Json, OFormat}


case class RegisteredAddress(
                              addressLine1: Option[String] = None,
                              addressLine2: Option[String] = None,
                              addressLine3: Option[String] = None,
                              postCode: Option[String] = None,
                              uprn: Option[String] = None
                            )

object RegisteredAddress {
  implicit val format: OFormat[RegisteredAddress] = Json.format[RegisteredAddress]
}
