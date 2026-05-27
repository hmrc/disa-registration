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

package uk.gov.hmrc.disaregistration.connectors

import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentSubscriberRequest
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaregistration.utils.WiremockHelper.stubPut

class TaxEnrolmentsConnectorISpec extends BaseIntegrationSpec {

  val connector: TaxEnrolmentsConnector = app.injector.instanceOf[TaxEnrolmentsConnector]

  "TaxEnrolmentsConnector.subscribe" should {

    val subscribeUrl = s"/tax-enrolments/subscriptions/$testFormBundleId/subscriber"
    val request      = TaxEnrolmentSubscriberRequest(
      serviceName = "HMRC-DISA-ORG",
      callback = "http://localhost:1203/disa-registration/callback/subscriptions",
      etmpId = testString
    )

    "return Right(HttpResponse) when backend returns 204 No Content" in {
      stubPut(subscribeUrl, NO_CONTENT, "")

      val response = await(connector.subscribe(testFormBundleId, request))

      response match {
        case Right(httpResponse) =>
          httpResponse.status shouldBe NO_CONTENT
        case Left(_)             =>
          fail("Expected Right(HttpResponse) but got Left")
      }
    }

    "return Left(UpstreamErrorResponse) when backend returns 4xx/5xx" in {
      stubPut(subscribeUrl, BAD_REQUEST, "Bad request from Tax Enrolments stub")

      val response = await(connector.subscribe(testFormBundleId, request))

      response match {
        case Left(err) =>
          err.statusCode shouldBe BAD_REQUEST
        case Right(_)  =>
          fail("Expected Left(UpstreamErrorResponse) but got Right")
      }
    }
  }
}
