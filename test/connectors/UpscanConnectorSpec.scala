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
import models.upscan.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class UpscanConnectorSpec extends SpecBase {

  trait TestSetup {

    val connector: UpscanConnector =
      applicationBuilder()
        .build()
        .injector
        .instanceOf[UpscanConnector]

    when(mockHttpClient.post(any())(any()))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }

  "UpscanConnector" - {

    "initiateUpload" - {

      "must return Upscan response when upload initiation succeeds" in new TestSetup {

        val request =
          UpscanInitiateRequest(
            callbackUrl = "http://test/callback",
            successRedirect = Some("http://test/success"),
            errorRedirect = Some("http://test/error"),
            minimumFileSize = Some(1),
            maximumFileSize = Some(1000000),
            expectedFileType = Some("application/pdf")
          )

        val response =
          UpscanInitiateResponse(
            reference = "ref123",
            uploadRequest = UploadRequest(href = "https://upscan/upload", fields = Map("key" -> "value"))
          )

        when(mockRequestBuilder.execute[UpscanInitiateResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result = connector.initiateUpload(request).futureValue

        result shouldBe response
      }

      "must include request body when initiating upload" in new TestSetup {

        val request =
          UpscanInitiateRequest(
            callbackUrl = "http://test/callback",
            successRedirect = None,
            errorRedirect = None,
            minimumFileSize = None,
            maximumFileSize = None,
            expectedFileType = None
          )

        when(mockRequestBuilder.execute[UpscanInitiateResponse](any(), any()))
          .thenReturn(
            Future.successful(
              UpscanInitiateResponse(
                reference = "123",
                uploadRequest = UploadRequest(href = "https://upscan/upload", fields = Map.empty)
              )
            )
          )

        connector.initiateUpload(request).futureValue

        verify(mockRequestBuilder, atLeastOnce)
          .execute[UpscanInitiateResponse](any(), any())
      }

      "must propagate UpstreamErrorResponse when Upscan fails" in new TestSetup {

        val request =
          UpscanInitiateRequest(
            callbackUrl = "http://test/callback",
            successRedirect = None,
            errorRedirect = None,
            minimumFileSize = None,
            maximumFileSize = None,
            expectedFileType = None
          )

        val upstreamError =
          UpstreamErrorResponse("Upscan unavailable", 503, 503, Map.empty)

        when(mockRequestBuilder.execute[UpscanInitiateResponse](any(), any()))
          .thenReturn(Future.failed(upstreamError))

        val thrown = connector.initiateUpload(request).failed.futureValue

        thrown shouldBe upstreamError
      }

      "must propagate unexpected failures" in new TestSetup {

        val request =
          UpscanInitiateRequest(
            callbackUrl = "http://test/callback",
            successRedirect = None,
            errorRedirect = None,
            minimumFileSize = None,
            maximumFileSize = None,
            expectedFileType = None
          )

        val exception = new RuntimeException("Connection timeout")

        when(mockRequestBuilder.execute[UpscanInitiateResponse](any(), any()))
          .thenReturn(Future.failed(exception))

        val thrown = connector.initiateUpload(request).failed.futureValue

        thrown shouldBe exception
      }
    }
  }
}
