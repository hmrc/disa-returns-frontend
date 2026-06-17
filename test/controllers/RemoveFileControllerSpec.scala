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
import forms.RemoveFileFormProvider
import models.{FileUpload, FileUploadDetails, FileUploadStatus}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.StorageService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.RemoveFileView

import scala.concurrent.Future

class RemoveFileControllerSpec extends SpecBase {

  private val formProvider = new RemoveFileFormProvider()

  private val testReference       = "abc-123-reference"
  private val secondTestReference = "def-456-reference"
  private val testFileName        = "return.csv"

  private val successfulUpload = FileUpload(
    reference = testReference,
    status = FileUploadStatus.UpscanSuccess,
    fileUploadDetails = Some(FileUploadDetails(testFileName))
  )

  private val secondSuccessfulUpload = FileUpload(
    reference = secondTestReference,
    status = FileUploadStatus.UpscanSuccess,
    fileUploadDetails = Some(FileUploadDetails("return2.csv"))
  )

  private val monthlyReturn         = emptyMonthlyReturn.copy(fileUploads = Seq(successfulUpload))
  private val multiFileMonthlyReturn = emptyMonthlyReturn.copy(fileUploads = Seq(successfulUpload, secondSuccessfulUpload))

  "RemoveFileController onPageLoad" - {

    "must return OK and the correct view when the file reference is found" in {
      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveFileController.onPageLoad(testReference).url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[RemoveFileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(formProvider(testFileName), testReference, testFileName)(request, messages(application)).toString
      }
    }

    "must redirect to UploadedReportFilesController when the file reference is not found" in {
      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveFileController.onPageLoad("unknown-reference").url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadedReportFilesController.onPageLoad().url
      }
    }

    "must redirect to UploadedReportFilesController when the file upload has no file details" in {
      val uploadWithoutDetails = FileUpload(reference = testReference, status = FileUploadStatus.UpscanSuccess)
      val application          =
        applicationBuilder(monthlyReturn = Some(emptyMonthlyReturn.copy(fileUploads = Seq(uploadWithoutDetails)))).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveFileController.onPageLoad(testReference).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadedReportFilesController.onPageLoad().url
      }
    }

    "must redirect to UploadedReportFilesController when the file exists but has not reached a successful status" in {
      val createdUpload = FileUpload(
        reference = testReference,
        status = "CREATED",
        fileUploadDetails = Some(FileUploadDetails(testFileName))
      )
      val application   =
        applicationBuilder(monthlyReturn = Some(emptyMonthlyReturn.copy(fileUploads = Seq(createdUpload)))).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveFileController.onPageLoad(testReference).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadedReportFilesController.onPageLoad().url
      }
    }

    "must return OK and the correct view when the file has VALIDATION_SUCCESS status" in {
      val validatedUpload = FileUpload(
        reference = testReference,
        status = FileUploadStatus.ValidationSuccess,
        fileUploadDetails = Some(FileUploadDetails(testFileName))
      )
      val application     =
        applicationBuilder(monthlyReturn = Some(emptyMonthlyReturn.copy(fileUploads = Seq(validatedUpload)))).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveFileController.onPageLoad(testReference).url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[RemoveFileView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(formProvider(testFileName), testReference, testFileName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when no monthly return is found" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(GET, routes.RemoveFileController.onPageLoad(testReference).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "RemoveFileController onSubmit" - {

    "must delete the file and redirect to UploadedReportFilesController when the user selects Yes and other files remain" in {
      val mockStorageService = mock[StorageService]
      when(
        mockStorageService.deleteFileUploadForThisWindow(eqTo(testZReference), eqTo(testReference))(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val application = applicationBuilder(monthlyReturn = Some(multiFileMonthlyReturn))
        .overrides(bind[StorageService].toInstance(mockStorageService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemoveFileController.onSubmit(testReference).url)
            .withFormUrlEncodedBody("value" -> "yes")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadedReportFilesController.onPageLoad().url
        verify(mockStorageService).deleteFileUploadForThisWindow(eqTo(testZReference), eqTo(testReference))(
          any[HeaderCarrier]
        )
      }
    }

    "must delete the file and redirect to UploadFileController when the user removes the last remaining file" in {
      val mockStorageService = mock[StorageService]
      when(
        mockStorageService.deleteFileUploadForThisWindow(eqTo(testZReference), eqTo(testReference))(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn))
        .overrides(bind[StorageService].toInstance(mockStorageService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemoveFileController.onSubmit(testReference).url)
            .withFormUrlEncodedBody("value" -> "yes")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadFileController.onPageLoad().url
        verify(mockStorageService).deleteFileUploadForThisWindow(eqTo(testZReference), eqTo(testReference))(
          any[HeaderCarrier]
        )
      }
    }

    "must redirect to UploadedReportFilesController without deleting when the user selects No" in {
      val mockStorageService = mock[StorageService]

      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn))
        .overrides(bind[StorageService].toInstance(mockStorageService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemoveFileController.onSubmit(testReference).url)
            .withFormUrlEncodedBody("value" -> "no")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadedReportFilesController.onPageLoad().url
        verifyNoInteractions(mockStorageService)
      }
    }

    "must return BadRequest and errors when no option is selected" in {
      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request   = FakeRequest(POST, routes.RemoveFileController.onSubmit(testReference).url)
        val boundForm = formProvider(testFileName).bind(Map.empty[String, String])
        val view      = application.injector.instanceOf[RemoveFileView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(boundForm, testReference, testFileName)(request, messages(application)).toString
      }
    }

    "must redirect to UploadedReportFilesController when the file reference is not found" in {
      val mockStorageService = mock[StorageService]

      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn))
        .overrides(bind[StorageService].toInstance(mockStorageService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemoveFileController.onSubmit("unknown-reference").url)
            .withFormUrlEncodedBody("value" -> "yes")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UploadedReportFilesController.onPageLoad().url
        verifyNoInteractions(mockStorageService)
      }
    }

    "must redirect to Journey Recovery when no monthly return is found" in {
      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemoveFileController.onSubmit(testReference).url)
            .withFormUrlEncodedBody("value" -> "yes")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
