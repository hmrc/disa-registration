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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.disaregistration.models.etmpsubmission.{EtmpSubmission, ProviderDetails}
import uk.gov.hmrc.disaregistration.models.journeyData._

class EtmpSubmissionSpec extends AnyWordSpec with Matchers {

  "EtmpSubmission.apply" should {

    "successfully build an EtmpSubmission when businessVerification and registeredAddress are present" in {

      val journeyData = JourneyData(
        groupId = "group-1",
        businessVerification = Some(
          BusinessVerification(
            businessRegistrationPassed = Some(true),
            businessVerificationPassed = Some(true),
            ctUtr = Some("1234567890"),
            registeredAddress = Some(
              RegisteredAddress(
                addressLine1 = Some("Line 1"),
                addressLine2 = Some("Line 2"),
                postCode = Some("AA1 1AA"),
                uprn = Some("123456789")
              )
            ),
            companyName = Some("Test Ltd")
          )
        )
      )

      val result = EtmpSubmission(journeyData)

      result mustBe Right(
        EtmpSubmission(
          providerDetails = ProviderDetails("123456789")
        )
      )
    }

    "fail when businessVerification is missing" in {

      val journeyData = JourneyData(
        groupId = "group-1",
        businessVerification = None
      )

      val result = EtmpSubmission(journeyData)

      result mustBe Left("Missing businessVerification")
    }

    "fail when registeredAddress is missing" in {

      val journeyData = JourneyData(
        groupId = "group-1",
        businessVerification = Some(
          BusinessVerification(
            businessRegistrationPassed = None,
            businessVerificationPassed = None,
            ctUtr = None,
            registeredAddress = None,
            companyName = None
          )
        )
      )

      val result = EtmpSubmission(journeyData)

      result mustBe Left("Missing registeredAddress")
    }
  }

  "EtmpSubmission JSON format" should {

    "serialize and deserialize correctly" in {

      val submission = EtmpSubmission(
        providerDetails = ProviderDetails("123456789")
      )

      val json         = Json.toJson(submission)
      val deserialized = json.as[EtmpSubmission]

      deserialized mustBe submission
    }
  }
}
