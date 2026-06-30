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

package services

import base.SpecBase
import config.FrontendAppConfig
import connectors.UpscanConnector
import models.upscan.{UpscanInitiateRequest, UpscanInitiateResponse}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import utils.DateHelper

import scala.concurrent.Future

class UpscanServiceSpec extends SpecBase with MockitoSugar {

  private val dateHelper = new DateHelper(testReportingWindowClock)

  private val appConfig = mock[FrontendAppConfig]

  "UpscanService" - {

    "must initiate an upscan upload with the correct request payload" in {

      val connector = mock[UpscanConnector]

      when(appConfig.disaReturnsBackendBaseUrl)
        .thenReturn("http://backend")

      when(appConfig.host)
        .thenReturn("http://localhost:12804")

      when(appConfig.upscanMinFileSize)
        .thenReturn(testUpscanMinFileSize)

      when(appConfig.upscanMaxFileSize)
        .thenReturn(testUpscanMaxFileSize)

      when(appConfig.upscanAcceptedMimeTypes)
        .thenReturn("application/pdf")

      val expectedResponse = UpscanInitiateResponse(
        reference = "ref123",
        uploadRequest = null
      )

      when(connector.initiateUpload(any[UpscanInitiateRequest])(any()))
        .thenReturn(Future.successful(expectedResponse))

      val service = new UpscanService(connector, dateHelper, appConfig)

      val result = service.initiate(testZReference).futureValue

      result mustEqual expectedResponse

      val expectedRequest = UpscanInitiateRequest(
        callbackUrl =
          s"http://backend/disa-returns-backend/monthly/upscan/callback/$testZReference/${dateHelper.reportingWindowTaxYear}/${dateHelper.reportingWindowMonthNumber}",
        successRedirect = Some("http://localhost:12804/upscan/success"),
        errorRedirect = Some("http://localhost:12804/file-upload/error"),
        minimumFileSize = Some(testUpscanMinFileSize),
        maximumFileSize = Some(testUpscanMaxFileSize),
        expectedFileType = Some("application/pdf")
      )

      verify(connector).initiateUpload(eqTo(expectedRequest))(any())
    }
  }
}
