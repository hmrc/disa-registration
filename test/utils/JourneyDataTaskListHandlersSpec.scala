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

package utils

import play.api.libs.json._
import uk.gov.hmrc.disaregistration.models.YesNoAnswer
import uk.gov.hmrc.disaregistration.models.journeyData._
import uk.gov.hmrc.disaregistration.models.journeyData.certificatesofauthority.CertificatesOfAuthority
import uk.gov.hmrc.disaregistration.models.journeyData.certificatesofauthority.CertificatesOfAuthorityYesNo.Yes
import uk.gov.hmrc.disaregistration.models.journeyData.certificatesofauthority.FcaArticles.Article14
import uk.gov.hmrc.disaregistration.models.journeyData.certificatesofauthority.FinancialOrganisation.RegisteredFriendlySociety
import uk.gov.hmrc.disaregistration.models.journeyData.isaProducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import uk.gov.hmrc.disaregistration.models.journeyData.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import uk.gov.hmrc.disaregistration.models.journeyData.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaregistration.models.journeyData.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaregistration.models.journeyData.thirdparty.{ThirdParty, ThirdPartyOrganisations}

class JourneyDataTaskListHandlersSpec extends BaseUnitSpec {

  "JourneyData.taskListJourneyHandlers" should {

    "have a handler for every expected task" in {
      val expectedKeys = Set(
        "businessVerification",
        "organisationDetails",
        "isaProducts",
        "certificatesOfAuthority",
        "liaisonOfficers",
        "signatories",
        "thirdPartyOrganisations"
      )

      JourneyData.taskListJourneyHandlers.keySet shouldBe expectedKeys
    }

    "serialize and deserialize each task correctly" in {
      JourneyData.taskListJourneyHandlers.foreach { case (taskName, handler) =>
        taskName match {
          case "businessVerification" =>
            val original     = BusinessVerification(
              businessRegistrationPassed = Some(true),
              businessVerificationPassed = Some(false),
              ctUtr = Some("12345678"),
              registeredAddress = Some(
                RegisteredAddress(
                  addressLine1 = Some("test line 1"),
                  addressLine2 = Some("test line 2"),
                  addressLine3 = Some("test line 3"),
                  postCode = Some("PostCode")
                )
              ),
              companyName = Some(testString)
            )
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[BusinessVerification]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[BusinessVerification]])
            deserialized shouldBe original

          case "organisationDetails" =>
            val original     = OrganisationDetails(
              registeredToManageIsa = Some(true),
              zRefNumber = Some("Z1111"),
              tradingUsingDifferentName = Some(true),
              tradingName = Some(testString),
              fcaNumber = Some("123456"),
              registeredAddressCorrespondence = Some(true),
              correspondenceAddress = Some(
                CorrespondenceAddress(
                  addressLine1 = Some("test line 1"),
                  addressLine2 = Some("test line 2"),
                  addressLine3 = Some("test line 3"),
                  postCode = Some("PostCode")
                )
              )
            )
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[OrganisationDetails]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[OrganisationDetails]])
            deserialized shouldBe original

          case "isaProducts" =>
            val original     = IsaProducts(
              Some(IsaProduct.values),
              Some(InnovativeFinancialProduct.values),
              Some(testString),
              Some(testString)
            )
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[IsaProducts]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[IsaProducts]])
            deserialized shouldBe original

          case "certificatesOfAuthority" =>
            val original     = CertificatesOfAuthority(
              certificatesYesNo = Some(Yes),
              fcaArticles = Some(Seq(Article14)),
              financialOrganisation = Some(Seq(RegisteredFriendlySociety))
            )
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[CertificatesOfAuthority]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[CertificatesOfAuthority]])
            deserialized shouldBe original

          case "liaisonOfficers" =>
            val original     =
              LiaisonOfficers(
                Seq(LiaisonOfficer(testString, Some(testString), Some(testString), Set(ByEmail), Some(testString)))
              )
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[LiaisonOfficers]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[LiaisonOfficers]])
            deserialized shouldBe original

          case "signatories" =>
            val original     = Signatories(Seq(Signatory(testString, Some(testString), Some(testString))))
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[Signatories]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[Signatories]])
            deserialized shouldBe original

          case "thirdPartyOrganisations" =>
            val original     = ThirdPartyOrganisations(
              Some(YesNoAnswer.Yes),
              Seq(ThirdParty(testString, Some(testString), Some(true), Some(true), Some(1))),
              Set.empty
            )
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[ThirdPartyOrganisations]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[ThirdPartyOrganisations]])
            deserialized shouldBe original

          case other =>
            fail(s"Unexpected task handler: $other")
        }
      }
    }
  }
}
