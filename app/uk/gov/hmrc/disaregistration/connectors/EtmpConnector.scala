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

import play.api.libs.json.Json
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EtmpConnector @Inject() (http: HttpClientV2, appConfig: AppConfig)(implicit val ec: ExecutionContext) {

  def declareAndSubmit(
    enrolmentSubmission: JourneyData
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, String]] = {
    val url = s"${appConfig.etmpBaseUrl}/etmp/enrolment/submission"
    http
      .post(url"$url")
      .withBody(Json.toJson(enrolmentSubmission))
      .execute[Either[UpstreamErrorResponse, String]]
  }
}
