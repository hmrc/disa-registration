package repositories

import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.repositories.RegistrationRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

import java.time.{Clock, Instant, ZoneOffset}

class RegistrationRepositorySpec extends BaseUnitSpec {

  protected val databaseName: String          = "disa-registration-test"
  protected val mongoUri: String              = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)
  private val appConfig                       = app.injector.instanceOf[AppConfig]
  val fixedClock: Clock                       = Clock.fixed(Instant.parse("2025-10-21T10:00:00Z"), ZoneOffset.UTC)

  val repository = new RegistrationRepository(mockMongoComponent, appConfig, fixedClock)

  override def beforeEach(): Unit = await(repository.collection.drop().toFuture())

  "findRegistrationById" should {
    "return a registration when found" in {
      await(repository.collection.insertOne(registration).toFuture())
      await(repository.findRegistrationById(groupId = groupId)) shouldBe Some(registration)
    }

    "return None when not found" in {
      await(repository.findRegistrationById(groupId = groupId)) shouldBe None
    }

    "upsert" should {
      "insert or update a registration and return the updated document" in {
        val fcaNumber                  = Some("FCA12345")
        val updatedOrganisationDetails = organisationDetails.copy(fcaNumber = fcaNumber)
        val updatedRegistration        = registration.copy(
          organisationDetails = updatedOrganisationDetails,
          lastUpdated = Some(Instant.now(fixedClock))
        )

        await(repository.upsert(groupId, registration))
        await(repository.upsert(groupId, updatedRegistration))
        await(repository.findRegistrationById(groupId))
          .map { reg =>
            reg.organisationDetails.fcaNumber              shouldBe fcaNumber
            reg.id                                         shouldBe groupId
            reg.organisationDetails.ZRefNumber             shouldBe organisationDetails.ZRefNumber
            reg.organisationDetails.registeredToManageIsas shouldBe organisationDetails.registeredToManageIsas
            reg                                            shouldBe updatedRegistration
          }
      }
    }
  }
}
