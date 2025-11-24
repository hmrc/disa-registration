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

package repositories

import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.models.journeyData.{BusinessVerification, CertificatesOfAuthority, OrganisationDetails}
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

import java.time.{Clock, Instant, ZoneOffset}

class JourneyAnswersRepositorySpec extends BaseUnitSpec {

  protected val databaseName: String          = "disa-journeyData-test"
  protected val mongoUri: String              = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)
  val fixedClock: Clock                       = Clock.fixed(Instant.parse("2025-10-21T10:00:00Z"), ZoneOffset.UTC)
  val repository: JourneyAnswersRepository    = new JourneyAnswersRepository(mockMongoComponent, mockAppConfig, fixedClock)

  override def beforeEach(): Unit = await(repository.collection.drop().toFuture())

  "findById" should {
    "return journeyData when found" in {
      await(repository.collection.insertOne(testJourneyData).toFuture())
      await(repository.findById(groupId = groupId)) shouldBe Some(testJourneyData)
    }

    "return None when not found" in {
      await(repository.findById(groupId = groupId)) shouldBe None
    }
  }

  "storeJourneyData" should {

    "successfully upsert a new document when none exists for this groupId" in {
      val model = BusinessVerification(
        dataItem = Some("TEST-ITEM"),
        dataItem2 = None
      )

      await(repository.storeJourneyData(groupId, "businessVerification", model))

      val result = await(repository.findById(groupId))
      result.get.businessVerification shouldBe Some(model)
      result.get.lastUpdated          shouldBe Some(Instant.now(fixedClock))
    }

    "successfully updates the existing document with the provided tasklist data" in {
      await(repository.collection.insertOne(testJourneyData).toFuture())
      val organisationDetailsUpdate =
        OrganisationDetails(registeredToManageIsa = Some(true), zRefNumber = Some("Z1234"))

      await(repository.storeJourneyData(groupId, "organisationDetails", organisationDetailsUpdate))

      val result = await(repository.findById(groupId)).get
      result shouldBe testJourneyData
        .copy(organisationDetails = Some(organisationDetailsUpdate))
        .copy(lastUpdated = Some(Instant.now(fixedClock)))
    }

    "successfully updates the journey document with the provided tasklist data - CertificatesOfAuthority " in {
      val coaJourney = CertificatesOfAuthority(
        dataItem = Some("test-data-item"),
        dataItem2 = Some("test-data-item-2")
      )

      await(repository.storeJourneyData(groupId, "certificatesOfAuthority", coaJourney))
      val result = await(repository.findById(groupId))
      result.get.certificatesOfAuthority shouldBe Some(coaJourney)
    }
  }
}
