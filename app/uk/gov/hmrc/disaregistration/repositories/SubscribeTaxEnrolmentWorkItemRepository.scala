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

package uk.gov.hmrc.disaregistration.repositories

import org.bson.types.ObjectId
import org.mongodb.scala.ClientSession
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentWorkItem
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem, WorkItemFields, WorkItemRepository}

import java.time.{Clock, Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscribeTaxEnrolmentWorkItemRepository @Inject() (
  clock: Clock,
  config: AppConfig,
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends WorkItemRepository[TaxEnrolmentWorkItem](
      collectionName = "submissionEtmpEnrolmentWorkItems",
      mongoComponent = mongoComponent,
      itemFormat = TaxEnrolmentWorkItem.format,
      workItemFields = WorkItemFields.default
    ) {
  override def now(): Instant =
    clock.instant()

  override val inProgressRetryAfter: Duration =
    config.subscriptionTaxEnrolmentJobInProgressRetryAfter

  def enqueue(
    formBundleId: String,
    bpSafeId: String
  )(implicit session: ClientSession): Future[WorkItem[TaxEnrolmentWorkItem]] = {
    val item     = TaxEnrolmentWorkItem(formBundleId, bpSafeId)
    val workItem = WorkItem(
      id = new ObjectId(),
      receivedAt = now(),
      updatedAt = now(),
      availableAt = now(),
      status = ProcessingStatus.ToDo,
      failureCount = 0,
      item = item
    )
    collection.insertOne(session, workItem).toFuture().map(_ => workItem)
  }
}
