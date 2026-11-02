/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.disaregistration.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Application, inject}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.models.journeyData.orgdetails.OrganisationDetails
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec
import com.github.tomakehurst.wiremock.client.WireMock.*

import scala.concurrent.Future

class JourneyDataRepositoryISpec extends BaseIntegrationSpec with MockitoSugar {
  // Inject failures at the repository boundary so HTTP error handling is exercised without stopping MongoDB.
  private lazy val repository        = mock[JourneyAnswersRepository]
  override lazy val app: Application = app(inject.bind[JourneyAnswersRepository].toInstance(repository))

  override def beforeEach(): Unit = {
    super.beforeEach()
    org.mockito.Mockito.reset(repository)
    stubAuth()
  }

  "Journey endpoints" should {
    "return 500 when journey retrieval fails" in {
      when(repository.findById(testGroupId)).thenReturn(Future.failed(new RuntimeException("Database unavailable")))
      val response = await(
        ws.url(s"http://localhost:$port/disa-registration/store/$testGroupId")
          .withHttpHeaders(testHeaders: _*)
          .get()
      )
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 when journey creation fails" in {
      when(repository.getOrCreateJourneyData(testGroupId))
        .thenReturn(Future.failed(new RuntimeException("Database unavailable")))
      val response = await(
        ws.url(s"http://localhost:$port/disa-registration/journey/$testGroupId")
          .withHttpHeaders(testHeaders: _*)
          .put(Json.obj())
      )
      response.status       shouldBe INTERNAL_SERVER_ERROR
      response.body[String] shouldBe "There has been an issue processing your request"
    }

    "return 500 when journey update fails" in {
      when(
        repository.updateJourneyData[OrganisationDetails](
          eqTo(testGroupId),
          eqTo("organisationDetails"),
          any[OrganisationDetails]
        )(any[Writes[OrganisationDetails]])
      )
        .thenReturn(Future.failed(new RuntimeException("Database unavailable")))
      val response = await(
        ws.url(s"http://localhost:$port/disa-registration/store/$testGroupId/organisationDetails")
          .withHttpHeaders(testHeaders: _*)
          .post(Json.toJson(organisationDetails))
      )
      response.status       shouldBe INTERNAL_SERVER_ERROR
      response.body[String] shouldBe "There has been an issue processing your request"
    }

    "return 500 without calling downstream services when journey data retrieval fails" in {
      when(repository.findById(testGroupId)).thenReturn(Future.failed(new RuntimeException("Database unavailable")))
      val response = await(
        ws.url(s"http://localhost:$port/disa-registration/$testGroupId/declare-and-submit")
          .withHttpHeaders(testHeaders: _*)
          .post(Json.obj())
      )
      response.status       shouldBe INTERNAL_SERVER_ERROR
      response.body[String] shouldBe "There has been an issue processing your request"
      verify(0, postRequestedFor(urlEqualTo("/etmp/enrolment/submission")))
      verify(0, putRequestedFor(urlMatching("/tax-enrolments/.*")))
    }
  }
}
