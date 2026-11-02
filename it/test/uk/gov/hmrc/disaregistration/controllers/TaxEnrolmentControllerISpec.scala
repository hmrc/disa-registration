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

package uk.gov.hmrc.disaregistration.controllers

import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec

class TaxEnrolmentControllerISpec extends BaseIntegrationSpec {
  private def url = s"http://localhost:$port/disa-registration/callback/subscriptions/$testFormBundleId"

  "POST /callback/subscriptions/:formBundleId" should {
    Seq("SUCCEEDED", "ERROR", "Enrolled", "AuthRefreshed", "EnrolmentError").foreach { state =>
      Seq(None, Some("subscription error")).foreach { error =>
        s"return 204 for $state with errorResponse $error" in {
          val body = Json.obj("url" -> "http://localhost/subscription", "state" -> state) ++
            error.fold(Json.obj())(value => Json.obj("errorResponse" -> value))
          await(ws.url(url).post(body)).status shouldBe NO_CONTENT
        }
      }
    }

    Seq(
      "unknown state"    -> Json.obj("url" -> "http://localhost/subscription", "state" -> "UNKNOWN"),
      "missing state"    -> Json.obj("url" -> "http://localhost/subscription"),
      "missing url"      -> Json.obj("state" -> "SUCCEEDED"),
      "wrong state type" -> Json.obj("url" -> "http://localhost/subscription", "state" -> 1),
      "wrong error type" -> Json.obj("url" -> "http://localhost/subscription", "state" -> "ERROR", "errorResponse" -> 1)
    ).foreach { case (description, body) =>
      s"return 400 for $description" in {
        await(ws.url(url).post(body)).status shouldBe BAD_REQUEST
      }
    }

    Seq("", "not JSON").foreach { body =>
      s"return 400 for an empty or non-JSON body '$body'" in {
        await(ws.url(url).post(body)).status shouldBe BAD_REQUEST
      }
    }

    "return 400 for malformed JSON" in {
      await(ws.url(url).withHttpHeaders("Content-Type" -> "application/json").post("{")).status shouldBe BAD_REQUEST
    }
  }
}
