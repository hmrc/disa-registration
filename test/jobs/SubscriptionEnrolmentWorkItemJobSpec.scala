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

package jobs

import org.apache.pekko.actor.ActorSystem
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.jobs.SubscriptionEnrolmentWorkItemJob
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentWorkItem
import uk.gov.hmrc.disaregistration.repositories.SubscribeTaxEnrollmentWorkItemRepository
import uk.gov.hmrc.disaregistration.service.TaxEnrolmentService
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.lang.reflect.Method
import java.time.Clock
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SubscriptionEnrolmentWorkItemJobSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterAll {

  private val actorSystem: ActorSystem = ActorSystem("SubscriptionEnrolmentWorkItemJobSpec")

  override def afterAll(): Unit = {
    Await.ready(actorSystem.terminate(), 5.seconds)
    super.afterAll()
  }

  private val clock               = Clock.systemUTC()
  private val lifecycle           = mock[ApplicationLifecycle]
  private val appConfig           = mock[AppConfig]
  private val workItemRepository  = mock[SubscribeTaxEnrollmentWorkItemRepository]
  private val taxEnrolmentService = mock[TaxEnrolmentService]

  when(appConfig.subscriptionTaxEnrollmentJobPollInterval).thenReturn(1.minute)

  private def newJob(): SubscriptionEnrolmentWorkItemJob =
    new SubscriptionEnrolmentWorkItemJob(
      actorSystem = actorSystem,
      clock = clock,
      lifecycle = lifecycle,
      appConfig = appConfig,
      workItemRepository = workItemRepository,
      taxEnrolmentService = taxEnrolmentService
    )

  private val formBundleId = "formBundle-123"
  private val bpSafeId     = "bpSafe-456"

  val workItem: WorkItem[TaxEnrolmentWorkItem] = {
    val item = TaxEnrolmentWorkItem(formBundleId = formBundleId, bpSafeId = bpSafeId)
    WorkItem(
      id = new ObjectId(),
      receivedAt = clock.instant(),
      updatedAt = clock.instant(),
      availableAt = clock.instant(),
      status = ProcessingStatus.InProgress,
      failureCount = 0,
      item = item
    )
  }

  // processWorkItem is protected on the base class, so it is invoked reflectively.
  private def invokeProcessWorkItem(
    job: SubscriptionEnrolmentWorkItemJob,
    workerId: Int,
    item: WorkItem[TaxEnrolmentWorkItem]
  ): Future[Boolean] = {
    val method: Method = classOf[SubscriptionEnrolmentWorkItemJob].getDeclaredMethods
      .find(_.getName == "processWorkItem")
      .getOrElse(throw new NoSuchMethodException("processWorkItem"))
    method.setAccessible(true)
    method
      .invoke(job, Int.box(workerId), item)
      .asInstanceOf[Future[Boolean]]
  }

  "processWorkItem" should {

    "mark the work item as Succeeded and return true when the subscription succeeds" in {
      reset(taxEnrolmentService, workItemRepository)
      val job = newJob()

      when(taxEnrolmentService.subscribe(eqTo(formBundleId), eqTo(bpSafeId))(any()))
        .thenReturn(Future.successful(()))
      when(workItemRepository.markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Succeeded), any()))
        .thenReturn(Future.successful(true))

      val result = invokeProcessWorkItem(job, workerId = 1, item = workItem).futureValue

      result shouldBe true
      verify(taxEnrolmentService).subscribe(eqTo(formBundleId), eqTo(bpSafeId))(any())
      verify(workItemRepository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Succeeded), any())
      verify(workItemRepository, never).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Failed), any())
    }

    "mark the work item as Failed but still return true when the subscription fails" in {
      reset(taxEnrolmentService, workItemRepository)
      val job = newJob()

      when(taxEnrolmentService.subscribe(eqTo(formBundleId), eqTo(bpSafeId))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))
      when(workItemRepository.markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Failed), any()))
        .thenReturn(Future.successful(true))
      when(workItemRepository.markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Succeeded), any()))
        .thenReturn(Future.successful(true))

      val result = invokeProcessWorkItem(job, workerId = 2, item = workItem).futureValue

      result shouldBe true
      verify(taxEnrolmentService).subscribe(eqTo(formBundleId), eqTo(bpSafeId))(any())
      verify(workItemRepository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Failed), any())
    }
  }

  "jobName" should {
    "be the repository job name" in {
      val job   = newJob()
      val field = classOf[SubscriptionEnrolmentWorkItemJob].getDeclaredMethods
        .find(_.getName == "jobName")
        .getOrElse(throw new NoSuchMethodException("jobName"))
      field.setAccessible(true)
      field.invoke(job) shouldBe "SubscribeTaxEnrollmentWorkItemRepository"
    }
  }
}
