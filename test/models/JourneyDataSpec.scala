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

package models

import play.api.libs.json.{Format, JsValue, Json}
import uk.gov.hmrc.disaregistration.models.journeyData.EnrolmentStatus.Active
import uk.gov.hmrc.disaregistration.models.journeyData._
import uk.gov.hmrc.disaregistration.models.journeyData.certificatesofauthority.FcaArticles.Article14
import uk.gov.hmrc.disaregistration.models.journeyData.certificatesofauthority.CertificatesOfAuthorityYesNo.Yes
import uk.gov.hmrc.disaregistration.models.journeyData.certificatesofauthority.{CertificatesOfAuthority, FinancialOrganisation}
import uk.gov.hmrc.disaregistration.models.journeyData.isaProducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import uk.gov.hmrc.disaregistration.models.journeyData.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import uk.gov.hmrc.disaregistration.models.journeyData.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import utils.JsonFormatSpec

class JourneyDataSpec extends JsonFormatSpec[JourneyData] {

  "JourneyData" should {
    "default status to Active and generate enrolmentId on construction" in {
      val jd = JourneyData(groupId = testGroupId)

      jd.groupId              shouldBe testGroupId
      jd.status               shouldBe Active
      jd.enrolmentId.nonEmpty shouldBe true

      jd.receiptId                    shouldBe None
      jd.feesCommissionsAndIncentives shouldBe None
      jd.businessVerification         shouldBe None
      jd.isaProducts                  shouldBe None
      jd.organisationDetails          shouldBe None
      jd.certificatesOfAuthority      shouldBe None
      jd.liaisonOfficers              shouldBe None
      jd.signatories                  shouldBe None
      jd.outsourcedAdministration     shouldBe None
      jd.lastUpdated                  shouldBe None
    }
  }

  override val model: JourneyData =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testEnrolmentId,
      receiptId = Some(testReceiptId),
      status = EnrolmentStatus.Submitted,
      businessVerification = Some(
        BusinessVerification(
          Some(true),
          Some(true),
          None,
          registeredAddress = Some(
            RegisteredAddress(
              addressLine1 = Some("test line 1"),
              addressLine2 = Some("test line 2"),
              addressLine3 = Some("test line 3"),
              postCode = Some("PostCode")
            )
          ),
          Some(testString)
        )
      ),
      organisationDetails = Some(
        OrganisationDetails(
          registeredToManageIsa = Some(true),
          zRefNumber = Some(testZRef),
          tradingUsingDifferentName = Some(true),
          tradingName = Some(testString),
          fcaNumber = Some(testString),
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
      ),
      isaProducts = Some(
        IsaProducts(
          Some(IsaProduct.values),
          Some(InnovativeFinancialProduct.values),
          Some(testString),
          Some(testString)
        )
      ),
      certificatesOfAuthority = Some(
        CertificatesOfAuthority(
          certificatesYesNo = Some(Yes),
          fcaArticles = Some(Seq(Article14)),
          financialOrganisation = Some(Seq(FinancialOrganisation.values.head))
        )
      ),
      liaisonOfficers =
        Some(LiaisonOfficers(Seq(LiaisonOfficer(testString, Some(testString), Some(testString), Set(ByEmail))))),
      signatories = None,
      outsourcedAdministration = Some(OutsourcedAdministration(Some("O1"), Some("O2"))),
      feesCommissionsAndIncentives = Some(FeesCommissionsAndIncentives(Some("F1"), Some("F2")))
    )

  override val json: JsValue = Json.parse(
    s"""
       |{
       |  "groupId": "$testGroupId",
       |  "enrolmentId": "$testEnrolmentId",
       |  "receiptId": "$testReceiptId",
       |  "status": "Submitted",
       |  "businessVerification": { "businessRegistrationPassed": true, "businessVerificationPassed": true,
       |  "registeredAddress": {
       |      "addressLine1": "test line 1",
       |      "addressLine2": "test line 2",
       |      "addressLine3": "test line 3",
       |      "postCode": "PostCode"
       |    },
       |  "companyName": "$testString"
       |  },
       |  "organisationDetails": {
       |    "registeredToManageIsa": true,
       |    "zRefNumber": "$testZRef",
       |    "tradingUsingDifferentName": true,
       |    "tradingName": "$testString",
       |    "fcaNumber": "$testString",
       |    "registeredAddressCorrespondence": true,
       |    "correspondenceAddress": {
       |      "addressLine1": "test line 1",
       |      "addressLine2": "test line 2",
       |      "addressLine3": "test line 3",
       |      "postCode": "PostCode"
       |    }
       |  },
       |  "isaProducts": {
       |    "isaProducts": ["cashIsas","cashJuniorIsas","stocksAndSharesIsas","stocksAndSharesJuniorIsas","innovativeFinanceIsas"],
       |    "innovativeFinancialProducts": ["peerToPeerLoansAndHave36HPermissions","peerToPeerLoansUsingAPlatformWith36HPermissions","crowdfundedDebentures","longTermAssetFunds"],
       |    "p2pPlatform": "$testString",
       |    "p2pPlatformNumber": "$testString"
       |  },
       |  "certificatesOfAuthority": { "certificatesYesNo":"yes", "fcaArticles": ["article14"], "financialOrganisation":["europeanInstitution"]},
       |  "liaisonOfficers": {"liaisonOfficers":[{"id":"test","fullName":"test","phoneNumber":"test","communication":["byEmail"]} ]},
       |  "outsourcedAdministration": { "dataItem": "O1", "dataItem2": "O2" },
       |  "feesCommissionsAndIncentives": { "dataItem": "F1", "dataItem2": "F2" }
       |}
       |""".stripMargin
  )

  override implicit val format: Format[JourneyData] = JourneyData.format
}
