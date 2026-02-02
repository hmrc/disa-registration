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

package uk.gov.hmrc.disaregistration.repositories

import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model._
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.models.journeyData.EnrolmentStatus.{Active, Submitted}
import uk.gov.hmrc.disaregistration.models.journeyData.{EnrolmentStatus, JourneyData}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{Clock, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyAnswersRepository @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig, clock: Clock)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[JourneyData](
      mongoComponent = mongoComponent,
      collectionName = "journeyAnswers",
      domainFormat = JourneyData.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions().name("journeyAnswersTtl").expireAfter(appConfig.timeToLive, TimeUnit.DAYS)
        ),
        IndexModel(
          Indexes.ascending("groupId"),
          IndexOptions()
            .name("singleActiveEnrolmentPerGroupIdx")
            .unique(true)
            .partialFilterExpression(Filters.eq("status", Active))
        ),
        IndexModel(ascending("enrolmentId"), IndexOptions().name("enrolmentIdIdx").unique(true))
      ),
      replaceIndexes = true,
      extraCodecs = Codecs.playFormatSumCodecs(EnrolmentStatus.format)
    ) {

  def findById(groupId: String): Future[Option[JourneyData]] =
    collection
      .find(
        Filters.and(Filters.eq("groupId", groupId), Filters.eq("status", Active))
      )
      .headOption()

  def upsertJourneyData[A: Writes](
    groupId: String,
    objectPath: String,
    journeyData: A
  ): Future[Unit] =
    collection
      .updateOne(
        Filters.and(Filters.eq("groupId", groupId), Filters.eq("status", Active)),
        Updates.combine(
          Updates.setOnInsert("enrolmentId", UUID.randomUUID().toString),
          Updates.setOnInsert("status", Active),
          Updates.set(objectPath, Codecs.toBson(Json.toJson(journeyData))),
          Updates.set("lastUpdated", Instant.now(clock))
        ),
        new UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())

  def storeReceiptAndMarkSubmitted(groupId: String, receiptId: String): Future[Option[JourneyData]] =
    collection
      .findOneAndUpdate(
        Filters.and(Filters.eq("groupId", groupId), Filters.eq("status", Active)),
        Updates.combine(
          Updates.set("receiptId", receiptId),
          Updates.set("status", Submitted),
          Updates.set("lastUpdated", Instant.now(clock))
        )
      )
      .toFutureOption()
}
