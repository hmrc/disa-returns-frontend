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

package controllers

import base.SpecBase
import models.upscan.{UploadRequest, UpscanInitiateResponse}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{StorageService, UpscanService}
import views.html.UploadFileView
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.UploadViewModel

import scala.concurrent.Future

class UploadFileControllerSpec extends SpecBase {

  "UploadFile Controller" - {

    "must return OK and render the view when Upscan service succeeds" in {

      val mockUpscanService = mock[UpscanService]

      val upscanResponse = UpscanInitiateResponse(
        reference = "test-reference",
        uploadRequest = UploadRequest(
          href = "https://upscan/upload",
          fields = Map(
            "key"              -> "mock-key",
            "policy"           -> "mock-policy",
            "x-amz-algorithm"  -> "mock-algorithm",
            "x-amz-credential" -> "mock-credential",
            "x-amz-signature"  -> "mock-signature"
          )
        )
      )

      when(mockUpscanService.initiate(eqTo(testZReference))(any[HeaderCarrier]))
        .thenReturn(Future.successful(upscanResponse))

      val mockStorageService = mock[StorageService]

      when(
        mockStorageService.createFileUploadForThisWindow(eqTo(testZReference), eqTo(upscanResponse.reference))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(()))

      val application = applicationBuilder()
        .overrides(
          bind[UpscanService].toInstance(mockUpscanService),
          bind[StorageService].toInstance(mockStorageService)
        )
        .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.UploadFileController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UploadFileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(UploadViewModel(upscan = upscanResponse, error = None))(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and render the view with an error when Upscan service succeeds and errorCode is present" in {

      val mockUpscanService = mock[UpscanService]

      val upscanResponse = UpscanInitiateResponse(
        reference = "test-reference",
        uploadRequest = UploadRequest(
          href = "https://upscan/upload",
          fields = Map.empty
        )
      )

      when(mockUpscanService.initiate(eqTo(testZReference))(any[HeaderCarrier]))
        .thenReturn(Future.successful(upscanResponse))

      val mockStorageService = mock[StorageService]

      when(
        mockStorageService.createFileUploadForThisWindow(eqTo(testZReference), eqTo(upscanResponse.reference))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(()))

      val application = applicationBuilder()
        .overrides(
          bind[UpscanService].toInstance(mockUpscanService),
          bind[StorageService].toInstance(mockStorageService)
        )
        .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.UploadFileController.onPageLoad().url + "?errorCode=EntityTooLarge")

        val result = route(application, request).value

        val view = application.injector.instanceOf[UploadFileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          UploadViewModel(upscan = upscanResponse, error = Some("uploadFile.entityTooLarge"))
        )(request, messages(application)).toString
      }
    }

    "must return InternalServerError when Upscan service fails" in {

      val mockUpscanService = mock[UpscanService]

      when(mockUpscanService.initiate(eqTo(testZReference))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("Upscan failed")))

      val application = applicationBuilder()
        .overrides(bind[UpscanService].toInstance(mockUpscanService))
        .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.UploadFileController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }

    "must return InternalServerError when registering the file upload with the backend fails" in {

      val mockUpscanService = mock[UpscanService]

      val upscanResponse = UpscanInitiateResponse(
        reference = "test-reference",
        uploadRequest = UploadRequest(
          href = "https://upscan/upload",
          fields = Map.empty
        )
      )

      when(mockUpscanService.initiate(eqTo(testZReference))(any[HeaderCarrier]))
        .thenReturn(Future.successful(upscanResponse))

      val mockStorageService = mock[StorageService]

      when(
        mockStorageService.createFileUploadForThisWindow(eqTo(testZReference), eqTo(upscanResponse.reference))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.failed(new RuntimeException("createFileUpload failed")))

      val application = applicationBuilder()
        .overrides(
          bind[UpscanService].toInstance(mockUpscanService),
          bind[StorageService].toInstance(mockStorageService)
        )
        .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.UploadFileController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

  "onError" - {

    "must redirect to onPageLoad with the errorCode query parameter" in {

      val application = applicationBuilder().build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.UploadFileController.onError().url +
              "?errorCode=EntityTooLarge&key=some-key&errorMessage=some+message&errorRequestId=some-id&errorResource=some-resource"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.UploadFileController.onPageLoad().url + "?errorCode=EntityTooLarge"
      }
    }

    "must redirect to onPageLoad with errorCode=failed when no errorCode is present" in {

      val application = applicationBuilder().build()

      running(application) {

        val request = FakeRequest(GET, routes.UploadFileController.onError().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.UploadFileController.onPageLoad().url + "?errorCode=failed"
      }
    }
  }
}
