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
import play.api.test.Helpers.await
import uk.gov.hmrc.disaregistration.service.RegistrationService
import utils.BaseUnitSpec

import scala.concurrent.Future

class RegistrationServiceSpec extends BaseUnitSpec {

  val service = new RegistrationService(mockRepository)

  "store" should {
    "successfully store a registration" in {
      when(mockRepository.upsert(any(), any())).thenReturn(Future.successful(registration))
      await(service.store(groupId, registration)) shouldBe registration
    }
  }

  "retrieve" should {
    "successfully retrieve a registration" in {
      when(mockRepository.findRegistrationById(groupId)).thenReturn(Future.successful(Some(registration)))
      await(service.retrieve(groupId)) shouldBe Some(registration)
    }
    "return None if repository returns None" in {
      when(mockRepository.findRegistrationById(groupId)).thenReturn(Future.successful(None))
      await(service.retrieve(groupId)) shouldBe None
    }
  }
}
