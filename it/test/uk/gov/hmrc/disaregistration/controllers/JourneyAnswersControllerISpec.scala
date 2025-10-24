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

package uk.gov.hmrc.disaregistration.controllers

import play.api.http.Status.{NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.disaregistration.utils.BaseIntegrationSpec

class JourneyAnswersControllerISpec extends BaseIntegrationSpec {

  private lazy val registrationRepository = app.injector.instanceOf[JourneyAnswersRepository]
  val groupId                             = "test-group-id"

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(registrationRepository.collection.drop().toFuture())
  }

  val registrationJson: String =
    """{
      |  "id": "test-group-id",
      |  "organisationDetails": {
      |    "registeredToManageIsa": false,
      |    "zRefNumber": "Z1235",
      |    "fcaNumber": "6743765"
      |  },
      |  "lastUpdated": "2025-10-21T15:27:28.433131Z"
      |}""".stripMargin

  val body: JsValue = Json.parse(registrationJson)

  "GET /store/:groupId" should {

    "return 404 Not Found when registration data does not exist" in {
      val result = retrieveRegistrationRequest(groupId = groupId)

      result.status shouldBe NOT_FOUND
      result.body   shouldBe s"Registration not found for groupId: $groupId"
    }

    "return 200 OK when registration data exists" in {
      storeRegistrationRequest(groupId, body)
      val result = retrieveRegistrationRequest(groupId = groupId)

      result.status                                                               shouldBe OK
      (result.json \ "id").as[String]                                             shouldBe groupId
      (result.json \ "organisationDetails" \ "registeredToManageIsa").as[Boolean] shouldBe false
      (result.json \ "organisationDetails" \ "zRefNumber").as[String]             shouldBe "Z1235"
      (result.json \ "organisationDetails" \ "fcaNumber").as[String]              shouldBe "6743765"
    }

    "return 401 Unauthorized for an unauthorised request" in {
      stubAuthFail()
      val result = await(
        ws.url(s"http://localhost:$port/disa-registration/store/$groupId")
          .get()
      )

      result.status shouldBe UNAUTHORIZED
    }

  }

  "POST /store/:groupId" should {

    "return 200 OK when registration data is successfully stored" in {
      val result = storeRegistrationRequest(groupId, body)

      result.status shouldBe OK

      (result.json \ "id").as[String]                                             shouldBe groupId
      (result.json \ "organisationDetails" \ "registeredToManageIsa").as[Boolean] shouldBe false
      (result.json \ "organisationDetails" \ "zRefNumber").as[String]             shouldBe "Z1235"
      (result.json \ "organisationDetails" \ "fcaNumber").as[String]              shouldBe "6743765"

    }

  }
  def storeRegistrationRequest(
    groupId: String,
    body: JsValue,
    headers: Seq[(String, String)] = testHeaders
  ): WSResponse = {
    stubAuth()
    await(
      ws.url(s"http://localhost:$port/disa-registration/store/$groupId")
        .withHttpHeaders(headers: _*)
        .post(body)
    )
  }

  def retrieveRegistrationRequest(groupId: String, headers: Seq[(String, String)] = testHeaders): WSResponse = {
    stubAuth()
    await(
      ws.url(s"http://localhost:$port/disa-registration/store/$groupId")
        .withHttpHeaders(headers: _*)
        .get()
    )
  }

}
