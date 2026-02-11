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

package uk.gov.hmrc.disaregistration.service

import play.api.Logging
import play.api.libs.json.Writes
import uk.gov.hmrc.disaregistration.models.GetOrCreateEnrolmentResult
import uk.gov.hmrc.disaregistration.models.journeyData.JourneyData
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyAnswersService @Inject() (repository: JourneyAnswersRepository) extends Logging {

  def retrieve(groupId: String): Future[Option[JourneyData]] =
    repository.findById(groupId)

  def getOrCreateEnrolment(groupId: String): Future[GetOrCreateEnrolmentResult] =
    repository.getOrCreateEnrolment(groupId)

  def storeJourneyData[A: Writes](
    groupId: String,
    objectPath: String,
    model: A
  ): Future[Unit] =
    repository.upsertJourneyData(groupId, objectPath, model)

  def storeReceiptAndMarkSubmitted(groupId: String, receiptId: String)(implicit
    executionContext: ExecutionContext
  ): Future[String] = {
    logger.info(s"Storing receipt [$receiptId] for groupId [$groupId] and marking as submitted")
    repository.storeReceiptAndMarkSubmitted(groupId, receiptId).map(_ => receiptId)
  }
}
