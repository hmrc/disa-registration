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

package uk.gov.hmrc.disaregistration.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.Duration
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.JavaDurationOps

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  lazy val etmpBaseUrl: String = servicesConfig.baseUrl(serviceName = "etmp")

  lazy val selfBaseUrl: String = servicesConfig.baseUrl("self")

  lazy val taxEnrolmentsBaseUrl: String = servicesConfig.baseUrl(serviceName = "tax-enrolments")

  lazy val taxEnrolmentsServiceName: String = servicesConfig.getString("tax-enrolments.service-name")

  def taxEnrolmentsCallbackUrl(formBundleId: String): String =
    s"$selfBaseUrl/disa-registration/callback/subscriptions/$formBundleId"

  val subscriptionTaxEnrolmentJobPollInterval: FiniteDuration = config
    .getOptional[Duration]("registration-work-item-job.pollInterval")
    .getOrElse(Duration.ofSeconds(10))
    .toScala

  val subscriptionTaxEnrolmentJobInProgressRetryAfter: Duration = config
    .getOptional[Duration]("registration-work-item-job.inProgressRetryAfter")
    .getOrElse(Duration.ofMinutes(5))

  val subscriptionTaxEnrolmentJobFailedRetryAfter: Duration = config
    .getOptional[Duration]("registration-work-item-job.failedRetryAfter")
    .getOrElse(Duration.ofMinutes(5))

  lazy val timeToLive: Int = servicesConfig.getInt("mongodb.timeToLive")
}
