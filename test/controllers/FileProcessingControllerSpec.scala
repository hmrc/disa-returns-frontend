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
import models.FileUpload
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.StorageService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.FileProcessingView

import scala.concurrent.Future

class FileProcessingControllerSpec extends SpecBase {

  private val testReference = "test-reference"

  "FileProcessingController" - {

    "onPageLoad" - {

      "must return OK and render the view using the key query parameter as the reference" in {

        val application = applicationBuilder().build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.onPageLoad(Some(testReference)).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[FileProcessingView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(reference = testReference)(request, messages(application)).toString
        }
      }

      "must redirect to the generic file upload failed page when no key query parameter is present" in {

        val application = applicationBuilder().build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.onPageLoad(None).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.FileUploadErrorController.fileUploadFailed().url
        }
      }
    }

    "status" - {

      "must return OK with the file upload status when found" in {

        val mockStorageService = mock[StorageService]

        when(
          mockStorageService.getFileUploadForThisPeriod(eqTo(testZReference), eqTo(testReference))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(Some(FileUpload(reference = testReference, status = "VALIDATION_SUCCESS"))))

        val application = applicationBuilder()
          .overrides(bind[StorageService].toInstance(mockStorageService))
          .build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.status(testReference).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.obj("status" -> "VALIDATION_SUCCESS")
        }
      }

      "must return NotFound when the file upload cannot be found" in {

        val mockStorageService = mock[StorageService]

        when(
          mockStorageService.getFileUploadForThisPeriod(eqTo(testZReference), eqTo(testReference))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(None))

        val application = applicationBuilder()
          .overrides(bind[StorageService].toInstance(mockStorageService))
          .build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.status(testReference).url)

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }
    }
  }
}
