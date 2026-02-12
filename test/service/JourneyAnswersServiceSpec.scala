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

package service

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.libs.json.Writes
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.models.GetOrCreateJourneyData
import uk.gov.hmrc.disaregistration.service.JourneyAnswersService
import utils.BaseUnitSpec

import scala.concurrent.Future

class JourneyAnswersServiceSpec extends BaseUnitSpec {

  val service = new JourneyAnswersService(mockRepository)

  "updateJourneyData" should {
    "successfully update journeyData" in {

      when(mockRepository.updateJourneyData(any[String], any[String], any[Any])(any[Writes[Any]]))
        .thenReturn(Future.successful(Some(())))

      await(
        service.updateJourneyData(
          testGroupId,
          "organisationDetails",
          organisationDetails.copy(registeredToManageIsa = Some(false))
        )
      ) shouldBe Some(())
    }

    "return None if there is no data for that groupID" in {

      when(mockRepository.updateJourneyData(any[String], any[String], any[Any])(any[Writes[Any]]))
        .thenReturn(Future.successful(None))

      await(
        service.updateJourneyData(
          testGroupId,
          "organisationDetails",
          organisationDetails.copy(registeredToManageIsa = Some(false))
        )
      ) shouldBe None
    }
  }

  "retrieve" should {
    "successfully retrieve journeyData" in {
      when(mockRepository.findById(testGroupId)).thenReturn(Future.successful(Some(testJourneyData)))
      await(service.retrieve(testGroupId)) shouldBe Some(testJourneyData)
    }
    "return None if repository returns None" in {
      when(mockRepository.findById(testGroupId)).thenReturn(Future.successful(None))
      await(service.retrieve(testGroupId)) shouldBe None
    }
  }

  "getOrCreateJourneyData" should {

    "return the GetOrCreateJourneyData from the repository" in {
      val repoResult =
        GetOrCreateJourneyData(isNewEnrolment = true, journeyData = testJourneyData)

      when(mockRepository.getOrCreateEnrolment(eqTo(testGroupId)))
        .thenReturn(Future.successful(repoResult))

      await(service.getOrCreateJourneyData(testGroupId)) shouldBe repoResult
    }

    "propagate exception when the repository call fails" in {
      val ex = new RuntimeException("fubar")

      when(mockRepository.getOrCreateEnrolment(eqTo(testGroupId)))
        .thenReturn(Future.failed(ex))

      val thrown = await(service.getOrCreateJourneyData(testGroupId).failed)

      thrown shouldBe ex
    }
  }

  "storeReceiptAndMarkSubmitted" should {

    "return the receiptId when the repository call succeeds" in {
      when(mockRepository.storeReceiptAndMarkSubmitted(eqTo(testGroupId), eqTo(testReceiptId)))
        .thenReturn(Future.successful(()))

      val result = await(service.storeReceiptAndMarkSubmitted(testGroupId, testReceiptId))

      result shouldBe testReceiptId
    }

    "propagate exception when the repository call fails" in {
      val ex = new RuntimeException("mongo down")

      when(mockRepository.storeReceiptAndMarkSubmitted(eqTo(testGroupId), eqTo(testReceiptId)))
        .thenReturn(Future.failed(ex))

      val thrown = await(service.storeReceiptAndMarkSubmitted(testGroupId, testReceiptId).failed)

      thrown shouldBe ex
    }
  }
}
