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
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.models.Registration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyAnswersRepository @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig, clock: Clock)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[Registration](
      mongoComponent = mongoComponent,
      collectionName = "journeyAnswers",
      domainFormat = Registration.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions().name("journeyAnswersTtl").expireAfter(appConfig.timeToLive, TimeUnit.DAYS)
        ),
        IndexModel(ascending("id"), IndexOptions().name("idIdx").unique(true))
      )
    ) {

  def findRegistrationById(groupId: String): Future[Option[Registration]] =
    collection.find(Filters.eq("id", groupId)).headOption()

  def upsert(groupId: String, registration: Registration): Future[Registration] = {
    val now = Instant.now(clock)

    val doc = registration.copy(id = groupId, lastUpdated = Some(now))
    collection
      .replaceOne(Filters.eq("id", groupId), doc, ReplaceOptions().upsert(true))
      .toFuture()
      .map(_ => doc)
  }
}
