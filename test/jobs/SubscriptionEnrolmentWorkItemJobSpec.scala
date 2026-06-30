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
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.BAD_REQUEST
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.jobs.SubscriptionEnrolmentWorkItemJob
import uk.gov.hmrc.disaregistration.models.taxenrolments.TaxEnrolmentWorkItem
import uk.gov.hmrc.disaregistration.repositories.SubscribeTaxEnrollmentWorkItemRepository
import uk.gov.hmrc.disaregistration.service.TaxEnrolmentService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import utils.BaseUnitSpec

import java.time.{Clock, Duration, Instant, ZoneOffset}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}

class SubscriptionEnrolmentWorkItemJobSpec extends BaseUnitSpec {

  private val now              = Instant.parse("2026-06-08T12:00:00Z")
  private val failedRetryAfter = Duration.ofMinutes(5)
  private val upstreamError    = UpstreamErrorResponse(
    message = "Bad request",
    statusCode = BAD_REQUEST,
    reportAs = BAD_REQUEST,
    headers = Map.empty
  )

  "SubscriptionEnrolmentWorkItemJob.processWorkItem" should {

    "mark Succeeded and return true when Tax Enrolments subscription succeeds" in new Fixture {
      when(taxEnrolmentService.subscribe(eqTo(testFormBundleId), eqTo(testString))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(())))

      job.process(workItem).futureValue shouldBe true

      verify(repository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Succeeded), any[Option[Instant]])
    }

    "mark Failed and return true when Tax Enrolments returns an upstream error" in new Fixture {
      when(taxEnrolmentService.subscribe(eqTo(testFormBundleId), eqTo(testString))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(upstreamError)))

      job.process(workItem).futureValue shouldBe true

      verify(repository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Failed), any[Option[Instant]])
    }

    "mark Failed and return true when Tax Enrolments subscription fails unexpectedly" in new Fixture {
      when(taxEnrolmentService.subscribe(eqTo(testFormBundleId), eqTo(testString))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      job.process(workItem).futureValue shouldBe true
      verify(repository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Failed), any[Option[Instant]])
    }

    "not complete until Tax Enrolments subscription has completed" in new Fixture {
      val promise = Promise[Either[UpstreamErrorResponse, Unit]]()
      when(taxEnrolmentService.subscribe(eqTo(testFormBundleId), eqTo(testString))(any[HeaderCarrier]))
        .thenReturn(promise.future)

      val result = job.process(workItem)
      result.isCompleted shouldBe false

      promise.success(Right(()))
      result.futureValue shouldBe true
    }

    "still return true when marking the work item returns false" in new Fixture(markAsResult = false) {
      when(taxEnrolmentService.subscribe(eqTo(testFormBundleId), eqTo(testString))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(())))

      job.process(workItem).futureValue shouldBe true
      verify(repository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Succeeded), any[Option[Instant]])
    }
  }

  private class Fixture(markAsResult: Boolean = true) {
    val workItem: WorkItem[TaxEnrolmentWorkItem] = WorkItem(
      id = new ObjectId(),
      receivedAt = now,
      updatedAt = now,
      availableAt = now,
      status = ProcessingStatus.ToDo,
      failureCount = 0,
      item = TaxEnrolmentWorkItem(testFormBundleId, testString)
    )

    val repository: SubscribeTaxEnrollmentWorkItemRepository = mock[SubscribeTaxEnrollmentWorkItemRepository]
    when(repository.markAs(any[ObjectId], any[ProcessingStatus], any[Option[Instant]]))
      .thenReturn(Future.successful(markAsResult))

    val taxEnrolmentService: TaxEnrolmentService = mock[TaxEnrolmentService]

    private val appConfig: AppConfig = mock[AppConfig]
    when(appConfig.subscriptionTaxEnrollmentJobPollInterval).thenReturn(1.hour)
    when(appConfig.subscriptionTaxEnrollmentJobFailedRetryAfter).thenReturn(failedRetryAfter)

    val job = new TestableSubscriptionEnrolmentWorkItemJob(repository, taxEnrolmentService, appConfig)
  }

  private class TestableSubscriptionEnrolmentWorkItemJob(
    repository: SubscribeTaxEnrollmentWorkItemRepository,
    taxEnrolmentService: TaxEnrolmentService,
    appConfig: AppConfig
  ) extends SubscriptionEnrolmentWorkItemJob(
        actorSystem = app.injector.instanceOf[ActorSystem],
        clock = Clock.fixed(now, ZoneOffset.UTC),
        lifecycle = mock[ApplicationLifecycle],
        appConfig = appConfig,
        workItemRepository = repository,
        taxEnrolmentService = taxEnrolmentService
      ) {
    def process(workItem: WorkItem[TaxEnrolmentWorkItem]): Future[Boolean] =
      processWorkItem(workerId = 1, workItem = workItem)
  }
}
