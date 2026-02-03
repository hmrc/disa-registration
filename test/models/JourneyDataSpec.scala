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
import uk.gov.hmrc.disaregistration.models.journeyData._
import uk.gov.hmrc.disaregistration.models.journeyData.isaProducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import utils.JsonFormatSpec

class JourneyDataSpec extends JsonFormatSpec[JourneyData] {

  override val model: JourneyData =
    JourneyData(
      groupId = testGroupId,
      enrolmentId = testEnrolmentId,
      receiptId = Some(testReceiptId),
      status = EnrolmentStatus.Submitted,
      businessVerification = Some(BusinessVerification(Some("A"), Some("B"))),
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
       |  "businessVerification": { "dataItem": "A", "dataItem2": "B" },
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
