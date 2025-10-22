package service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.service.RegistrationService
import utils.BaseUnitSpec

import scala.concurrent.Future

class RegistrationServiceSpec extends BaseUnitSpec {

  val service = new RegistrationService(mockRepository)

  "store" should {
    "successful store a registration" in {
      when(mockRepository.upsert(any(), any())).thenReturn(Future.successful(registration))
      await(service.store(groupId, registration)) shouldBe registration
    }
  }

  "retrieve" should {
    "successful retrieve a registration" in {
      when(mockRepository.findRegistrationById(groupId)).thenReturn(Future.successful(Some(registration)))
      await(service.retrieve(groupId)) shouldBe Some(registration)
    }
    "return None if repository returns None" in {
      when(mockRepository.findRegistrationById(groupId)).thenReturn(Future.successful(None))
      await(service.retrieve(groupId)) shouldBe None
    }
  }
}
