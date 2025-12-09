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

package models.isaProducts

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.disaregistration.models.journeyData.isaProducts.IsaProduct
import utils.{BaseUnitSpec, ModelGenerators}

class IsaProductSpec extends BaseUnitSpec with ModelGenerators with ScalaCheckPropertyChecks with OptionValues {

  "IsaProduct" should {

    "must deserialise valid values" in {

      val gen = arbitrary[IsaProduct]

      forAll(gen) { isaProducts =>
        JsString(isaProducts.toString).validate[IsaProduct].asOpt.value mustEqual isaProducts
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!IsaProduct.values.map(_.toString).contains(_))

      forAll(gen) { invalidValue =>
        JsString(invalidValue).validate[IsaProduct] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = arbitrary[IsaProduct]

      forAll(gen) { isaProducts =>
        Json.toJson(isaProducts) mustEqual JsString(isaProducts.toString)
      }
    }
  }
}
