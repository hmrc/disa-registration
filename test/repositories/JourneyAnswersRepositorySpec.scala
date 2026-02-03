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

import org.mongodb.scala.model.Filters
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.models.journeyData.EnrolmentStatus.{Active, Submitted}
import uk.gov.hmrc.disaregistration.models.journeyData.{BusinessVerification, CertificatesOfAuthority, JourneyData, OrganisationDetails}
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

import java.time.{Clock, Instant, ZoneOffset}

class JourneyAnswersRepositorySpec extends BaseUnitSpec {

  protected val databaseName: String          = "disa-journeyData-test"
  protected val mongoUri: String              = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  val fixedClock: Clock                    = Clock.fixed(Instant.parse("2025-10-21T10:00:00Z"), ZoneOffset.UTC)
  val repository: JourneyAnswersRepository = new JourneyAnswersRepository(mockMongoComponent, mockAppConfig, fixedClock)

  override def beforeEach(): Unit = await(repository.collection.drop().toFuture())

  private def activeJourneyData: JourneyData =
    testJourneyData.copy(
      status = Active,
      receiptId = None,
      lastUpdated = None
    )

  private def submittedJourneyData: JourneyData =
    testJourneyData.copy(
      status = Submitted,
      receiptId = Some(testReceiptId),
      lastUpdated = None
    )

  "findById" should {

    "return journeyData when found and status is Active" in {
      await(repository.collection.insertOne(activeJourneyData).toFuture())
      await(repository.findById(groupId = testGroupId)) shouldBe Some(activeJourneyData)
    }

    "return None when not found" in {
      await(repository.findById(groupId = testGroupId)) shouldBe None
    }

    "return None when a document exists but status is not Active" in {
      await(repository.collection.insertOne(submittedJourneyData).toFuture())
      await(repository.findById(groupId = testGroupId)) shouldBe None
    }
  }

  "upsertJourneyData" should {

    "successfully upsert a new Active document when none exists for this groupId" in {
      val model = BusinessVerification(
        dataItem = Some(testString),
        dataItem2 = None
      )

      await(repository.upsertJourneyData(testGroupId, "businessVerification", model))

      val result = await(repository.findById(testGroupId)).get

      result.groupId              shouldBe testGroupId
      result.status               shouldBe Active
      result.receiptId            shouldBe None
      result.enrolmentId.nonEmpty shouldBe true
      result.businessVerification shouldBe Some(model)
      result.lastUpdated          shouldBe Some(Instant.now(fixedClock))
    }

    "successfully updates the existing Active document with the provided data" in {
      await(repository.collection.insertOne(activeJourneyData).toFuture())

      val organisationDetailsUpdate =
        OrganisationDetails(registeredToManageIsa = Some(true), zRefNumber = Some(testZRef))

      await(repository.upsertJourneyData(testGroupId, "organisationDetails", organisationDetailsUpdate))

      val result = await(repository.findById(testGroupId)).get

      result.groupId             shouldBe activeJourneyData.groupId
      result.enrolmentId         shouldBe activeJourneyData.enrolmentId
      result.status              shouldBe Active
      result.organisationDetails shouldBe Some(organisationDetailsUpdate)
      result.lastUpdated         shouldBe Some(Instant.now(fixedClock))
    }

    "upserts and stores CertificatesOfAuthority data" in {
      val coaJourney = CertificatesOfAuthority(
        dataItem = Some(testString),
        dataItem2 = Some(testString)
      )

      await(repository.upsertJourneyData(testGroupId, "certificatesOfAuthority", coaJourney))

      val result = await(repository.findById(testGroupId)).get

      result.certificatesOfAuthority shouldBe Some(coaJourney)
      result.status                  shouldBe Active
    }

    "creates a new Active document when only a non-Active document exists for the groupId" in {
      await(repository.collection.insertOne(submittedJourneyData).toFuture())

      val model = BusinessVerification(
        dataItem = Some("SHOULD-NOT-UPDATE"),
        dataItem2 = None
      )

      await(repository.upsertJourneyData(testGroupId, "businessVerification", model))

      val active = await(repository.findById(testGroupId)).get
      active.status               shouldBe Active
      active.businessVerification shouldBe Some(model)
      active.receiptId            shouldBe None
      active.lastUpdated          shouldBe Some(Instant.now(fixedClock))

      val allForGroup = await(repository.collection.find(Filters.eq("groupId", testGroupId)).toFuture())
      allForGroup.size shouldBe 2

      val submitted = allForGroup.find(_.status == Submitted).get
      submitted.status               shouldBe Submitted
      submitted.receiptId            shouldBe submittedJourneyData.receiptId
      submitted.businessVerification shouldBe submittedJourneyData.businessVerification

      active.enrolmentId should not equal submitted.enrolmentId
    }
  }

  "storeReceiptAndMarkSubmitted" should {

    "stores receiptId, sets status to Submitted and updates lastUpdated when an Active document exists" in {
      await(repository.collection.insertOne(activeJourneyData).toFuture())

      await(repository.storeReceiptAndMarkSubmitted(testGroupId, testReceiptId))

      await(repository.findById(testGroupId)) shouldBe None

      val stored = await(
        repository.collection.find(Filters.eq("groupId", testGroupId)).headOption()
      ).get

      stored.status      shouldBe Submitted
      stored.receiptId   shouldBe Some(testReceiptId)
      stored.lastUpdated shouldBe Some(Instant.now(fixedClock))
    }

    "fails when no Active document exists for the groupId" in {
      val err = await(repository.storeReceiptAndMarkSubmitted(testGroupId, testReceiptId).failed)
      err          shouldBe a[NoSuchElementException]
      err.getMessage should include(s"Failed to find document to mark Submitted for groupId [$testGroupId]")
    }

    "fails when only a non-Active document exists for the groupId" in {
      await(repository.collection.insertOne(submittedJourneyData).toFuture())

      val err = await(repository.storeReceiptAndMarkSubmitted(testGroupId, testString).failed)
      err          shouldBe a[NoSuchElementException]
      err.getMessage should include(s"Failed to find document to mark Submitted for groupId [$testGroupId]")
    }
  }
}
