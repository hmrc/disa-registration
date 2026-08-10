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

package uk.gov.hmrc.disaregistration.models.journeyData

import uk.gov.hmrc.disaregistration.models.{Enumerable, WithName}

sealed trait GrsCompanyType

object GrsCompanyType extends Enumerable.Implicits {

  case object LimitedCompany extends WithName("limitedCompany") with GrsCompanyType
  case object EuropeanInstitutionWithAUkBase extends WithName("europeanInstitutionWithAUkBase") with GrsCompanyType
  case object IncorporatedFriendlySociety extends WithName("incorporatedFriendlySociety") with GrsCompanyType
  case object RegisteredFriendlySociety extends WithName("registeredFriendlySociety") with GrsCompanyType
  case object GeneralPartnership extends WithName("generalPartnership") with GrsCompanyType
  case object ScottishPartnership extends WithName("scottishPartnership") with GrsCompanyType
  case object ScottishLimitedPartnership extends WithName("scottishLimitedPartnership") with GrsCompanyType
  case object LimitedPartnership extends WithName("limitedPartnership") with GrsCompanyType
  case object LimitedLiabilityPartnership extends WithName("limitedLiabilityPartnership") with GrsCompanyType

  val values: Seq[GrsCompanyType] = Seq(
    LimitedCompany,
    EuropeanInstitutionWithAUkBase,
    IncorporatedFriendlySociety,
    RegisteredFriendlySociety,
    GeneralPartnership,
    ScottishPartnership,
    ScottishLimitedPartnership,
    LimitedPartnership,
    LimitedLiabilityPartnership
  )

  implicit val enumerable: Enumerable[GrsCompanyType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
