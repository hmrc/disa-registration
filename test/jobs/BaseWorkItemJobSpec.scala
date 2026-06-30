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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{timeout => mockitoTimeout, verify, when}
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.disaregistration.jobs.BaseWorkItemJob
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemRepository}
import utils.BaseUnitSpec

import java.time.{Clock, Duration, Instant, ZoneOffset}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}

class BaseWorkItemJobSpec extends BaseUnitSpec {

  private val now              = Instant.parse("2026-06-08T12:00:00Z")
  private val failedRetryAfter = Duration.ofMinutes(5)
  private val workerCount      = math.max(1, Runtime.getRuntime.availableProcessors() / 2)

  "BaseWorkItemJob.start" should {

    "poll for outstanding work items using the configured failed retry delay" in new Fixture {
      when(repository.pullOutstanding(any[Instant], any[Instant]))
        .thenReturn(Future.successful(None))

      job.start()

      verify(repository, mockitoTimeout(1000).times(workerCount))
        .pullOutstanding(eqTo(now.minus(failedRetryAfter)), eqTo(now))
    }

    "process an outstanding work item" in new Fixture {
      when(repository.pullOutstanding(any[Instant], any[Instant]))
        .thenReturn(Future.successful(Some(workItem)), Future.successful(None))

      job.start()

      processedWorkItem.future.futureValue shouldBe workItem
    }
  }

  private class Fixture {
    val repository: WorkItemRepository[String]       = mock[WorkItemRepository[String]]
    val processedWorkItem: Promise[WorkItem[String]] = Promise[WorkItem[String]]()
    val workItem: WorkItem[String]                   = dummyWorkItem("test-item")

    val job: BaseWorkItemJob[String] = new BaseWorkItemJob[String](
      actorSystem = app.injector.instanceOf[ActorSystem],
      clock = Clock.fixed(now, ZoneOffset.UTC),
      lifecycle = mock[ApplicationLifecycle],
      workItemRepository = repository,
      dispatcherName = "contexts.registration-work-item",
      pollInterval = 1.hour,
      failedRetryAfter = failedRetryAfter
    ) {
      override protected val jobName: String = "TestWorkItemJob"

      override protected def processWorkItem(workerId: Int, workItem: WorkItem[String]): Future[Boolean] = {
        processedWorkItem.trySuccess(workItem)
        Future.successful(false)
      }
    }
  }
}
