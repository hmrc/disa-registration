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

import uk.gov.hmrc.disaregistration.models.journeyData.EnrolmentStatus.Active
import uk.gov.hmrc.disaregistration.models.journeyData.{BusinessVerification, JourneyData, OrganisationDetails}

import java.util.UUID
import scala.util.Random

trait TestData {
  val testGroupId     = UUID.randomUUID().toString
  val testEnrolmentId = UUID.randomUUID().toString
  val testReceiptId   = UUID.randomUUID().toString
  val testString      = "test"
  val testZRef        = s"Z${(1 to 4).map(_ => Random.nextInt(10)).mkString}"

  val organisationDetails: OrganisationDetails =
    OrganisationDetails(registeredToManageIsa = Some(true), zRefNumber = Some("Z1234"))

  val businessVerification: BusinessVerification = BusinessVerification(
    businessRegistrationPassed = Some(true),
    businessVerificationPassed = Some(false),
    ctUtr = Some("12345678")
  )
  val testJourneyData: JourneyData               = JourneyData(
    groupId = testGroupId,
    enrolmentId = testEnrolmentId,
    receiptId = None,
    status = Active,
    organisationDetails = Some(organisationDetails),
    businessVerification = Some(businessVerification)
  )
}
