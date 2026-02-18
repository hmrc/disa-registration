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
import uk.gov.hmrc.disaregistration.models.journeyData.isaProducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
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
      businessVerification = Some(BusinessVerification(Some(true), Some(true), None)),
      organisationDetails = Some(
        OrganisationDetails(
          registeredToManageIsa = Some(true),
          zRefNumber = Some(testZRef),
          tradingUsingDifferentName = Some(true),
          tradingName = Some(testString),
          fcaNumber = Some(testString),
          None
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
      certificatesOfAuthority = Some(CertificatesOfAuthority(Some("C"), Some("D"))),
      liaisonOfficers = Some(LiaisonOfficers(Some("L"), Some("LO"))),
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
       |  "businessVerification": { "businessRegistrationPassed": true, "businessVerificationPassed": true },
       |  "organisationDetails": {
       |    "registeredToManageIsa": true,
       |    "zRefNumber": "$testZRef",
       |    "tradingUsingDifferentName": true,
       |    "tradingName": "$testString",
       |    "fcaNumber": "$testString"
       |  },
       |  "isaProducts": {
       |    "isaProducts": ["cashIsas","cashJuniorIsas","stocksAndSharesIsas","stocksAndSharesJuniorIsas","innovativeFinanceIsas"],
       |    "innovativeFinancialProducts": ["peerToPeerLoansAndHave36HPermissions","peerToPeerLoansUsingAPlatformWith36HPermissions","crowdfundedDebentures","longTermAssetFunds"],
       |    "p2pPlatform": "$testString",
       |    "p2pPlatformNumber": "$testString"
       |  },
       |  "certificatesOfAuthority": { "dataItem": "C", "dataItem2": "D" },
       |  "liaisonOfficers": { "dataItem": "L", "dataItem2": "LO" },
       |  "outsourcedAdministration": { "dataItem": "O1", "dataItem2": "O2" },
       |  "feesCommissionsAndIncentives": { "dataItem": "F1", "dataItem2": "F2" }
       |}
       |""".stripMargin
  )

  override implicit val format: Format[JourneyData] = JourneyData.format
}
