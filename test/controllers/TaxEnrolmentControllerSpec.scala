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

package controllers

import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disaregistration.controllers.routes.TaxEnrolmentController
import utils.BaseUnitSpec

class TaxEnrolmentControllerSpec extends BaseUnitSpec {

  "TaxEnrolmentController.callback" should {

    "return NO_CONTENT when valid SUCCEEDED payload is submitted" in {

      val request =
        FakeRequest(POST, TaxEnrolmentController.callback(testFormBundleId).url)
          .withJsonBody(
            Json.obj(
              "url"   -> "http://localhost:1203/disa-registration/callback/subscriptions/123456789012",
              "state" -> "SUCCEEDED"
            )
          )

      running(fakeApplication()) {
        val result = route(fakeApplication(), request).get

        status(result) shouldBe NO_CONTENT

      }
    }

    "return NO_CONTENT when valid ERROR payload is submitted" in {

      val request =
        FakeRequest(POST, TaxEnrolmentController.callback(testFormBundleId).url)
          .withJsonBody(
            Json.obj(
              "url"           -> "http://localhost:1203/disa-registration/callback/subscriptions/123456789012",
              "state"         -> "ERROR",
              "errorResponse" -> "error message"
            )
          )

      running(fakeApplication()) {
        val result = route(fakeApplication(), request).get

        status(result) shouldBe NO_CONTENT

      }
    }

    "return BAD_REQUEST when request body is not JSON" in {

      running(fakeApplication()) {
        val request =
          FakeRequest(POST, TaxEnrolmentController.callback(testFormBundleId).url)
            .withTextBody("not-json")

        val result = route(fakeApplication(), request).get

        status(result)          shouldBe BAD_REQUEST
        contentAsString(result) shouldBe "Received tax enrolment callback with empty or non-JSON body"
      }
    }

    "return BAD_REQUEST when JSON payload is invalid" in {

      running(fakeApplication()) {
        val request =
          FakeRequest(POST, TaxEnrolmentController.callback(testFormBundleId).url)
            .withJsonBody(
              Json.obj(
                "url"   -> "http://localhost:1203/disa-registration/callback/subscriptions/123456789012",
                "state" -> "NOT_A_REAL_STATE"
              )
            )

        val result = route(fakeApplication(), request).get

        status(result) shouldBe BAD_REQUEST
      }
    }

  }
}
