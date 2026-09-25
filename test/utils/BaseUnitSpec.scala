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

package utils

import org.mockito.Mockito
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.disaregistration.config.AppConfig
import uk.gov.hmrc.disaregistration.connectors.{EtmpConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.disaregistration.repositories.JourneyAnswersRepository
import uk.gov.hmrc.disaregistration.service.{JourneyAnswersService, SubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.mongo.MongoComponent
import utils.TestData as DisaTestData

import java.time.Clock
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

abstract class BaseUnitSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with EitherValues
    with ScalaFutures
    with MockitoSugar
    with DefaultAwaitTimeout
    with GuiceOneAppPerSuite
    with DisaTestData {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockHttpClient: HttpClientV2                       = mock[HttpClientV2]
  val mockAppConfig: AppConfig                           = mock[AppConfig]
  val mockRequestBuilder: RequestBuilder                 = mock[RequestBuilder]
  val mockAuthConnector: AuthConnector                   = mock[AuthConnector]
  val mockRepository: JourneyAnswersRepository           = mock[JourneyAnswersRepository]
  val mockJourneyAnswersService: JourneyAnswersService   = mock[JourneyAnswersService]
  val mockSubmissionService: SubmissionService           = mock[SubmissionService]
  val mockEtmpConnector: EtmpConnector                   = mock[EtmpConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val mockBaseMongoComponent: MongoComponent             = mock[MongoComponent]
  val mockClock: Clock                                   = mock[Clock]

  protected val databaseName: String          = "disa-journeyData-test"
  protected val mongoUri: String              = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  override def beforeEach(): Unit = {
    val mocksToReset: Seq[AnyRef] = Seq(
      mockHttpClient,
      mockAppConfig,
      mockRequestBuilder,
      mockAuthConnector,
      mockRepository,
      mockJourneyAnswersService,
      mockSubmissionService,
      mockEtmpConnector,
      mockTaxEnrolmentsConnector,
      mockBaseMongoComponent,
      mockClock
    )
    Mockito.reset(mocksToReset: _*)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AppConfig].toInstance(mockAppConfig),
      bind[JourneyAnswersRepository].toInstance(mockRepository),
      bind[JourneyAnswersService].toInstance(mockJourneyAnswersService),
      bind[MongoComponent].toInstance(mockBaseMongoComponent),
      bind[Clock].toInstance(mockClock)
    )
    .build()

  protected def inject[T: ClassTag]: T =
    app.injector.instanceOf[T]

}
