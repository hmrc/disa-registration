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

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class JourneyData(
  groupId: String,
  businessVerification: Option[BusinessVerification] = None,
  organisationDetails: Option[OrganisationDetails] = None,
  isaProducts: Option[IsaProducts] = None,
  certificatesOfAuthority: Option[CertificatesOfAuthority] = None,
  liaisonOfficers: Option[LiaisonOfficers] = None,
  signatories: Option[Signatories] = None,
  outsourcedAdministration: Option[OutsourcedAdministration] = None,
  feesCommissionsAndIncentives: Option[FeesCommissionsAndIncentives] = None,
  lastUpdated: Option[Instant] = None
)

object JourneyData {
  implicit val instantFormat: Format[Instant] =
    Format(MongoJavatimeFormats.instantReads, MongoJavatimeFormats.instantWrites)

  implicit val format: Format[JourneyData] = Json.format[JourneyData]

  case class JourneyField[A](reads: Reads[A], writes: Writes[A])

  val fieldHandlers: Map[String, JourneyField[_]] = Map(
    "businessVerification"         -> JourneyField(BusinessVerification.format, BusinessVerification.format),
    "organisationDetails"          -> JourneyField(OrganisationDetails.format, OrganisationDetails.format),
    "isaProducts"                  -> JourneyField(IsaProducts.format, IsaProducts.format),
    "certificatesOfAuthority"      -> JourneyField(CertificatesOfAuthority.format, CertificatesOfAuthority.format),
    "liaisonOfficers"              -> JourneyField(LiaisonOfficers.format, LiaisonOfficers.format),
    "signatories"                  -> JourneyField(Signatories.format, Signatories.format),
    "outsourcedAdministration"     -> JourneyField(OutsourcedAdministration.format, OutsourcedAdministration.format),
    "feesCommissionsAndIncentives" -> JourneyField(
      FeesCommissionsAndIncentives.format,
      FeesCommissionsAndIncentives.format
    )
  )
}
