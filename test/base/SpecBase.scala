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

package base

import controllers.actions.*
import models.UserAnswers
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.http.HttpRequest
import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  val userAnswersId: String = "id"

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  protected val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  protected val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  protected def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  protected def messages(app: Application): Messages =
    app.injector
      .instanceOf[MessagesApi]
      .preferred(FakeRequest())

  protected def applicationBuilder(
    userAnswers: Option[UserAnswers] = None
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservices.upscan.base-url" -> "http://localhost:9999"
      )
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[HttpClientV2].toInstance(mockHttpClient),
        bind[RequestBuilder].toInstance(mockRequestBuilder)
      )
}
