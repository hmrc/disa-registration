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

package uk.gov.hmrc.disaregistration.jobs

import org.apache.pekko.actor.ActorSystem
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentWorkItem
import uk.gov.hmrc.disaregistration.repositories.SubscribeTaxEnrollmentWorkItemRepository
import uk.gov.hmrc.disaregistration.service.{SubmissionService, TaxEnrolmentService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.control.NonFatal
@Singleton
class SubscriptionEnrolmentWorkItemJob @Inject() (
  actorSystem: ActorSystem,
  clock: Clock,
  lifecycle: ApplicationLifecycle,
  appConfig: AppConfig,
  workItemRepository: SubscribeTaxEnrollmentWorkItemRepository,
  taxEnrolmentService: TaxEnrolmentService
) extends BaseWorkItemJob[TaxEnrolmentWorkItem](
      actorSystem = actorSystem,
      clock = clock,
      lifecycle = lifecycle,
      workItemRepository = workItemRepository,
      dispatcherName = "contexts.registration-work-item",
      pollInterval = appConfig.subscriptionTaxEnrollmentJobPollInterval
    ) {
  override protected val jobName: String = "SubscribeTaxEnrollmentWorkItemRepository"
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def processWorkItem(
    workerId: Int,
    workItem: WorkItem[TaxEnrolmentWorkItem]
  ): Future[Boolean] = {
    val bpSafeId     = workItem.item.bpSafeId
    val formBundleId = workItem.item.formBundleId
    logger.info(
      s"[SubscribeTaxEnrollmentWorkItemRepository][processWorkItem] Worker $workerId processing work item for " +
        s"formBundleId [$formBundleId], bpSafeId [$bpSafeId]."
    )

    taxEnrolmentService
      .subscribe(formBundleId, bpSafeId)
      .recover { case NonFatal(e) =>
        logger.error(
          s"Tax Enrolments subscription failed for formBundleId [$formBundleId] and bpSafeId [$bpSafeId]",
          e
        )
        workItemRepository.markAs(workItem.id, ProcessingStatus.Failed)
      }
      .map(_ => workItemRepository.markAs(workItem.id, ProcessingStatus.Succeeded))
      // Indicates a work item was pulled and handled/attempted.
      // It does not mean the file upload business processing succeeded.
      .map(_ => true)
  }

}
