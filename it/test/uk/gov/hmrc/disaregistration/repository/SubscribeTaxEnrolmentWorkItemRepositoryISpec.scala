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

package uk.gov.hmrc.disaregistration.repository

import java.time.Clock
import org.mongodb.scala.{ClientSession, SingleObservable, SingleObservableFuture}
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentWorkItem
import uk.gov.hmrc.disaregistration.repositories.SubscribeTaxEnrolmentWorkItemRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscribeTaxEnrolmentWorkItemRepositoryISpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with PlayMongoRepositorySupport[WorkItem[TaxEnrolmentWorkItem]]
    with IntegrationPatience
    with Transactions {

  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockClock: Clock         = Clock.systemUTC()

  override protected val repository: SubscribeTaxEnrolmentWorkItemRepository =
    new SubscribeTaxEnrolmentWorkItemRepository(mockClock, mockAppConfig, mongoComponent)

  implicit val tc: TransactionConfiguration = TransactionConfiguration.strict

  val testFormBundleId = "test-form-bundle-id"
  val testBpSafeId     = "test-bp-safe-id"

  "enqueue" should {

    "insert a new WorkItem with ToDo status" in {
      val workItem = withSessionAndTransaction[WorkItem[TaxEnrolmentWorkItem]] { implicit session =>
        repository.enqueue(testFormBundleId, testBpSafeId)
      }.futureValue

      workItem.item.formBundleId shouldBe testFormBundleId
      workItem.item.bpSafeId     shouldBe testBpSafeId
      workItem.status            shouldBe ProcessingStatus.ToDo

      val stored = repository.collection
        .find(Filters.equal("item.formBundleId", testFormBundleId))
        .headOption()
        .futureValue

      stored            shouldBe defined
      stored.get.status shouldBe ProcessingStatus.ToDo
    }

    "not persist the WorkItem if the transaction is rolled back" in {
      val rollbackFormBundleId   = "rollback-test-form-bundle-id"
      val session: ClientSession = mongoComponent.client.startSession().toFuture().futureValue
      try {
        session.startTransaction()

        val result = (for {
          _ <- repository.enqueue(rollbackFormBundleId, testBpSafeId)(session)
          _ <- Future.failed[Unit](new RuntimeException("simulated failure after enqueue"))
        } yield ()).recoverWith { case _ =>
          SingleObservable(session.abortTransaction()).toFuture().map(_ => ())
        }

        result.futureValue
      } finally session.close()

      val stored = repository.collection
        .find(Filters.equal("item.formBundleId", rollbackFormBundleId))
        .headOption()
        .futureValue

      stored shouldBe None
    }
  }
}
