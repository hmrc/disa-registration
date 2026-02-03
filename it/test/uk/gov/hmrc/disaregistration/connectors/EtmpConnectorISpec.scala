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

import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.models.EnrolmentSubmissionResponse
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaregistration.utils.WiremockHelper.stubPost
import uk.gov.hmrc.http.JsValidationException

class EtmpConnectorISpec extends BaseIntegrationSpec {

  val connector: EtmpConnector = app.injector.instanceOf[EtmpConnector]

  "EtmpConnector.declareAndSubmit" should {

    val declareAndSubmitUrl     = "/etmp/enrolment/submission"
    val submission: JourneyData = testJourneyData

    "return Right(EnrolmentSubmissionResponse) when backend returns 200 OK with valid json" in {
      val responseBody = Json.toJson(EnrolmentSubmissionResponse(testString)).toString
      stubPost(declareAndSubmitUrl, OK, responseBody)

      val response = await(connector.declareAndSubmit(submission))

      response shouldBe Right(EnrolmentSubmissionResponse(testString))
    }

    "return Left(UpstreamErrorResponse) when backend returns 4xx/5xx and the http client materialises it as Left" in {
      val responseBody =
        """{"statusCode":401,"message":"Not authorised"}"""
      stubPost(declareAndSubmitUrl, UNAUTHORIZED, responseBody)

      val response = await(connector.declareAndSubmit(submission))

      response match {
        case Left(err) =>
          err.statusCode shouldBe UNAUTHORIZED
        case Right(_)  =>
          fail("Expected Left(UpstreamErrorResponse) but got Right")
      }
    }

    "propagate exception when the call fails with bad json" in {
      stubPost(declareAndSubmitUrl, OK, """{"json":"bad"}""")

      val err = await(connector.declareAndSubmit(submission).failed)

      err shouldBe an[JsValidationException]
    }
  }
}
