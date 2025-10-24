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
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

import java.time.{Clock, Instant, ZoneOffset}

class JourneyAnswersRepositorySpec extends BaseUnitSpec {

  protected val databaseName: String          = "disa-registration-test"
  protected val mongoUri: String              = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)
  private val appConfig                       = app.injector.instanceOf[AppConfig]
  val fixedClock: Clock                       = Clock.fixed(Instant.parse("2025-10-21T10:00:00Z"), ZoneOffset.UTC)

  val repository = new JourneyAnswersRepository(mockMongoComponent, appConfig, fixedClock)

  override def beforeEach(): Unit = await(repository.collection.drop().toFuture())

  "findRegistrationById" should {
    "return registration data when found" in {
      await(repository.collection.insertOne(registration).toFuture())
      await(repository.findRegistrationById(groupId = groupId)) shouldBe Some(registration)
    }

    "return None when not found" in {
      await(repository.findRegistrationById(groupId = groupId)) shouldBe None
    }

    "upsert" should {
      "insert or update registration data and return the updated document" in {
        val fcaNumber                  = Some("FCA12345")
        val updatedOrganisationDetails = organisationDetails.copy(fcaNumber = fcaNumber)
        val updatedRegistration        = registration.copy(
          organisationDetails = Some(updatedOrganisationDetails),
          lastUpdated = Some(Instant.now(fixedClock))
        )

        await(repository.upsert(groupId, registration))
        await(repository.upsert(groupId, updatedRegistration))
        await(repository.findRegistrationById(groupId)).map(_ shouldBe updatedRegistration)

      }
    }
  }
}
