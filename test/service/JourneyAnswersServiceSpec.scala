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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Writes
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.service.JourneyAnswersService
import utils.BaseUnitSpec

import scala.concurrent.Future

class JourneyAnswersServiceSpec extends BaseUnitSpec {

  val service = new JourneyAnswersService(mockRepository)

  "upsertJourneyData" should {
    "successfully store journeyData" in {

      when(mockRepository.upsertJourneyData(any[String], any[String], any[Any])(any[Writes[Any]]))
        .thenReturn(Future.successful(()))

      await(
        service.storeJourneyData(
          groupId,
          "organisationDetails",
          organisationDetails.copy(registeredToManageIsa = Some(false))
        )
      ) shouldBe (): Unit
    }
  }

  "retrieve" should {
    "successfully retrieve journeyData" in {
      when(mockRepository.findById(groupId)).thenReturn(Future.successful(Some(testJourneyData)))
      await(service.retrieve(groupId)) shouldBe Some(testJourneyData)
    }
    "return None if repository returns None" in {
      when(mockRepository.findById(groupId)).thenReturn(Future.successful(None))
      await(service.retrieve(groupId)) shouldBe None
    }
  }
}
