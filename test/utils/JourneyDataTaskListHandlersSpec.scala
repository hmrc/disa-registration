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
import uk.gov.hmrc.disaregistration.models.journeyData._
import uk.gov.hmrc.disaregistration.models.journeyData.isaProducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}

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
        "outsourcedAdministration",
        "feesCommissionsAndIncentives"
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
              ctUtr = Some("12345678")
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
              fcaNumber = Some("123456")
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
            val original     = CertificatesOfAuthority(Some(testString), None)
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[CertificatesOfAuthority]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[CertificatesOfAuthority]])
            deserialized shouldBe original

          case "liaisonOfficers" =>
            val original     = LiaisonOfficers(Some(testString), None)
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[LiaisonOfficers]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[LiaisonOfficers]])
            deserialized shouldBe original

          case "signatories" =>
            val original     = Signatories(Some(testString), None)
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[Signatories]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[Signatories]])
            deserialized shouldBe original

          case "outsourcedAdministration" =>
            val original     = OutsourcedAdministration(Some(testString), None)
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[OutsourcedAdministration]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[OutsourcedAdministration]])
            deserialized shouldBe original

          case "feesCommissionsAndIncentives" =>
            val original     = FeesCommissionsAndIncentives(Some(testString), None)
            val json         = Json.toJson(original)(handler.writes.asInstanceOf[Writes[FeesCommissionsAndIncentives]])
            val deserialized = json.as(handler.reads.asInstanceOf[Reads[FeesCommissionsAndIncentives]])
            deserialized shouldBe original

          case other =>
            fail(s"Unexpected task handler: $other")
        }
      }
    }
  }
}
