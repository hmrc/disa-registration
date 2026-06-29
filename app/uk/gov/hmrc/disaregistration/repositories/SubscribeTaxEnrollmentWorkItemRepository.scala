package uk.gov.hmrc.disaregistration.repositories

import org.mongodb.scala.model.Filters
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.models.etmpsubmission.EtmpSubmission
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemFields, WorkItemRepository}

import java.time.{Clock, Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscribeTaxEnrollmentWorkItemRepository @Inject() (
                                                           clock: Clock,
                                                           config: AppConfig,
                                                           mongoComponent: MongoComponent
                                                         )(implicit ec: ExecutionContext)
  extends WorkItemRepository[EtmpSubmission](
    collectionName = "submissionEtmpEnrollmentWorkItems",
    mongoComponent = mongoComponent,
    itemFormat = EtmpSubmission.format,
    workItemFields = WorkItemFields.default
  ){
  override def now(): Instant =
    clock.instant()

  override val inProgressRetryAfter: Duration =
    config.subscriptionTaxEnrollmentJobInProgressRetryAfter

  def enqueue(
               enrolment: JourneyData
             ): Future[WorkItem[EtmpSubmission]] =
    EtmpSubmission(enrolment) match {
      case Left(err) => Future.failed(new IllegalArgumentException(err))
      case Right(submission) => pushNew(submission)
    }

  def deleteAll(): Future[Long] =
    collection
      .deleteMany(Filters.empty())
      .toFuture()
      .map(_.getDeletedCount)
}
