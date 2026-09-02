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

package connectors

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_GATEWAY, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class BackendConnectorSpec extends SpecBase {
  "BackendConnector.isReportingWindowOpen" - {
    "must return the status supplied by backend" in {
      val appConfig = mock[FrontendAppConfig]
      val response  = mock[HttpResponse]
      when(appConfig.disaReturnsBackendBaseUrl).thenReturn("http://backend")
      when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))
      when(response.status).thenReturn(OK)
      when(response.json).thenReturn(Json.obj("reportingWindowOpen" -> false))

      val connector = new BackendConnector(mockHttpClient, appConfig)

      connector.isReportingWindowOpen(testZReference).futureValue mustBe false
    }

    "must fail when backend returns an error" in {
      val appConfig = mock[FrontendAppConfig]
      val response  = mock[HttpResponse]
      when(appConfig.disaReturnsBackendBaseUrl).thenReturn("http://backend")
      when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))
      when(response.status).thenReturn(BAD_GATEWAY)

      val connector = new BackendConnector(mockHttpClient, appConfig)

      connector.isReportingWindowOpen(testZReference).failed.futureValue mustBe a[UpstreamErrorResponse]
    }
  }
}
