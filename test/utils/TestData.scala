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

import uk.gov.hmrc.disaregistration.models.etmpsubmission.{EtmpSubmission, ProviderDetails}
import uk.gov.hmrc.disaregistration.models.journeyData.{BusinessVerification, CorrespondenceAddress, JourneyData, OrganisationDetails, RegisteredAddress}

import java.util.UUID
import scala.util.Random

trait TestData {
  val testGroupId             = UUID.randomUUID().toString
  val testEnrolmentId: String = UUID.randomUUID().toString
  val testReceiptId: String   = UUID.randomUUID().toString
  val testString              = "test"
  val testZRef                = s"Z${(1 to 4).map(_ => Random.nextInt(10)).mkString}"

  val organisationDetails: OrganisationDetails =
    OrganisationDetails(
      registeredToManageIsa = Some(true),
      zRefNumber = Some("Z1234"),
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

  val businessVerification: BusinessVerification = BusinessVerification(
    businessRegistrationPassed = Some(true),
    businessVerificationPassed = Some(false),
    ctUtr = Some("12345678"),
    registeredAddress = Some(
      RegisteredAddress(
        addressLine1 = Some("test line 1"),
        addressLine2 = Some("test line 2"),
        addressLine3 = Some("test line 3"),
        postCode = Some("PostCode"),
        uprn = Some(testString)
      )
    ),
    companyName = Some(testString),
    businessPartnerId = Some(testString)
  )
  val testJourneyData: JourneyData               = JourneyData(
    groupId = testGroupId,
    businessVerification = Some(businessVerification),
    organisationDetails = Some(organisationDetails),
    thirdPartyOrganisations = None
  )

  val testEtmpSubmission: EtmpSubmission = EtmpSubmission(providerDetails = ProviderDetails(uprn = testString))
}
