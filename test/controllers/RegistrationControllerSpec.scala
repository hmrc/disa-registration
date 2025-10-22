package controllers

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.disaregistration.controllers.RegistrationController
import utils.BaseUnitSpec

import scala.concurrent.Future

class RegistrationControllerSpec extends BaseUnitSpec {

  val controller: RegistrationController = app.injector.instanceOf[RegistrationController]
  val registrationJson: JsValue          = Json.toJson(registration)

  def authorisedUser(): Unit =
    when(mockAuthConnector.authorise(any, any[Retrieval[Unit]])(any, any)).thenReturn(Future.successful(()))

  "retrieve" should {
    "return 200 OK when registration data exists" in {
      authorisedUser()
      when(mockRegistrationService.retrieve(groupId)).thenReturn(Future.successful(Some(registration)))

      val result = controller.retrieve(groupId)(FakeRequest())

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe registrationJson
    }

    "return 404 Not Found when no registration data is found" in {
      authorisedUser()
      when(mockRegistrationService.retrieve(groupId)).thenReturn(Future.successful(None))

      val result = controller.retrieve(groupId)(FakeRequest())

      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include(s"Registration not found for groupId: $groupId")
    }
  }

  "store" should {
    "return 200 OK when a registration is stored successful" in {
      authorisedUser()
      when(mockRegistrationService.store(groupId, registration)).thenReturn(Future.successful(registration))

      val request = FakeRequest()
        .withBody(registrationJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(groupId)(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe registrationJson
    }

    "return 500 Internal Server Error" in {
      authorisedUser()
      when(mockRegistrationService.store(groupId, registration))
        .thenReturn(Future.failed(new RuntimeException("DB error")))

      val request = FakeRequest()
        .withBody(registrationJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.store(groupId)(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("There has been an issue processing your request")
    }

  }

}
