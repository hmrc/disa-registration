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

package models.taxenrolments

import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentCallbackState
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentCallbackState._
import utils.BaseUnitSpec

class TaxEnrolmentCallbackStateSpec extends BaseUnitSpec {

  "TaxEnrolmentCallbackState format" should {

    "serialise Succeeded" in {
      Json.toJson[TaxEnrolmentCallbackState](Succeeded) shouldBe JsString("SUCCEEDED")
    }

    "serialise Error" in {
      Json.toJson[TaxEnrolmentCallbackState](Error) shouldBe JsString("ERROR")
    }

    "serialise EnrolmentError" in {
      Json.toJson[TaxEnrolmentCallbackState](EnrolmentError) shouldBe JsString("EnrolmentError")
    }

    "serialise Enrolled" in {
      Json.toJson[TaxEnrolmentCallbackState](Enrolled) shouldBe JsString("Enrolled")
    }

    "serialise AuthRefreshed" in {
      Json.toJson[TaxEnrolmentCallbackState](AuthRefreshed) shouldBe JsString("AuthRefreshed")
    }

    "deserialise Succeeded" in {
      JsString("SUCCEEDED").as[TaxEnrolmentCallbackState] shouldBe Succeeded
    }

    "deserialise Error" in {
      JsString("ERROR").as[TaxEnrolmentCallbackState] shouldBe Error
    }

    "deserialise EnrolmentError" in {
      JsString("EnrolmentError").as[TaxEnrolmentCallbackState] shouldBe EnrolmentError
    }

    "deserialise Enrolled" in {
      JsString("Enrolled").as[TaxEnrolmentCallbackState] shouldBe Enrolled
    }

    "deserialise AuthRefreshed" in {
      JsString("AuthRefreshed").as[TaxEnrolmentCallbackState] shouldBe AuthRefreshed
    }

    "fail to deserialise an unknown state" in {
      JsString("fubar").validate[TaxEnrolmentCallbackState] shouldBe
        JsError("Invalid enrolment callback state: fubar")
    }

    "fail to deserialise a non-string value" in {
      Json.obj("x" -> 1).validate[TaxEnrolmentCallbackState] shouldBe
        JsError("State must be a string")
    }
  }
}
