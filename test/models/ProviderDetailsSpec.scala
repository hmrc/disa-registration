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
import uk.gov.hmrc.disaregistration.config.Constants
import uk.gov.hmrc.disaregistration.models.etmpsubmission.ProviderDetails
import uk.gov.hmrc.disaregistration.models.journeyData.RegisteredAddress

class ProviderDetailsSpec extends AnyWordSpec with Matchers {

  "ProviderDetails.fromJourneyAddress" should {

    "use the uprn from the journey address when it is present" in {

      val address = RegisteredAddress(
        uprn = Some("123456789")
      )

      val result = ProviderDetails.fromJourneyAddress(address)

      result.uprn mustBe "123456789"
    }

    "fallback to the default uprn when uprn is missing" in {

      val address = RegisteredAddress(
        uprn = None
      )

      val result = ProviderDetails.fromJourneyAddress(address)

      result.uprn mustBe Constants.defaultUprn
    }
  }

  "ProviderDetails JSON format" should {

    "serialize and deserialize correctly" in {

      val model = ProviderDetails("123456789")

      val json         = Json.toJson(model)
      val deserialized = json.as[ProviderDetails]

      deserialized mustBe model
    }
  }
}
