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
import models.{FileUpload, FileUploadDetails, FileUploadStatus, ValidationResult}
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

  private def applicationWithFileUpload(result: Future[Option[FileUpload]]) = {
    val mockStorageService = mock[StorageService]

    when(
      mockStorageService.getFileUploadForThisPeriod(eqTo(testZReference), eqTo(testReference))(
        any[HeaderCarrier]
      )
    ).thenReturn(result)

    applicationBuilder()
      .overrides(bind[StorageService].toInstance(mockStorageService))
      .build()
  }

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
          contentAsString(result) must include(messages(application)("fileProcessing.noJs.initial.heading"))
          contentAsString(result) must include(routes.FileProcessingController.checkProgress(testReference).url)
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

    "checkProgress" - {

      FileUploadStatus.pending.foreach { pendingStatus =>
        s"must show the still-being-checked variant without a spinner for $pendingStatus" in {
          val application =
            applicationWithFileUpload(Future.successful(Some(FileUpload(testReference, pendingStatus))))

          running(application) {
            val request = FakeRequest(GET, routes.FileProcessingController.checkProgress(testReference).url)
            val result  = route(application, request).value
            val content = contentAsString(result)

            status(result) mustEqual OK
            content must include(messages(application)("fileProcessing.noJs.still.heading"))
            content must not include "ccms-loader"
          }
        }
      }

      FileUploadStatus.terminal.foreach { terminalStatus =>
        s"must show the finished-being-checked variant without a spinner for $terminalStatus" in {
          val application =
            applicationWithFileUpload(Future.successful(Some(FileUpload(testReference, terminalStatus))))

          running(application) {
            val request = FakeRequest(GET, routes.FileProcessingController.checkProgress(testReference).url)
            val result  = route(application, request).value
            val content = contentAsString(result)

            status(result) mustEqual OK
            content must include(messages(application)("fileProcessing.noJs.finished.heading"))
            content must include(routes.FileProcessingController.onContinue(testReference).url)
            content must not include "ccms-loader"
          }
        }
      }

      "must redirect to the generic upload failure page for a missing or unknown upload" in {
        Seq(None, Some(FileUpload(testReference, "UNKNOWN"))).foreach { fileUpload =>
          val application = applicationWithFileUpload(Future.successful(fileUpload))

          running(application) {
            val request = FakeRequest(GET, routes.FileProcessingController.checkProgress(testReference).url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.FileUploadErrorController.fileUploadFailed().url
          }
        }
      }

      "must redirect to the generic upload failure page when retrieving the upload fails" in {
        val application = applicationWithFileUpload(Future.failed(new RuntimeException("failed")))

        running(application) {
          val request = FakeRequest(GET, routes.FileProcessingController.checkProgress(testReference).url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.FileUploadErrorController.fileUploadFailed().url
        }
      }
    }

    "onContinue" - {

      FileUploadStatus.pending.foreach { pendingStatus =>
        s"must show the still-being-checked variant when the upload remains $pendingStatus" in {
          val application =
            applicationWithFileUpload(Future.successful(Some(FileUpload(testReference, pendingStatus))))

          running(application) {
            val request = FakeRequest(GET, routes.FileProcessingController.onContinue(testReference).url)
            val result  = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result) must include(messages(application)("fileProcessing.noJs.still.heading"))
          }
        }
      }

      Seq(
        FileUploadStatus.UpscanRejected    -> (() => routes.FileUploadErrorController.invalidFileType().url),
        FileUploadStatus.Duplicate         -> (() => routes.FileUploadErrorController.duplicateFileUpload().url),
        FileUploadStatus.ValidationSuccess -> (() => routes.UploadedReportFilesController.onPageLoad().url),
        FileUploadStatus.ValidationFailure -> (() => routes.FileUploadErrorController.fileUploadFailed().url),
        FileUploadStatus.UpscanUnknown     -> (() => routes.FileUploadErrorController.fileUploadFailed().url),
        FileUploadStatus.UpscanExpired     -> (() => routes.FileUploadErrorController.fileUploadFailed().url)
      ).foreach { case (uploadStatus, expectedUrl) =>
        s"must redirect $uploadStatus to the correct page" in {
          val application =
            applicationWithFileUpload(Future.successful(Some(FileUpload(testReference, uploadStatus))))

          running(application) {
            val request = FakeRequest(GET, routes.FileProcessingController.onContinue(testReference).url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual expectedUrl()
          }
        }
      }

      "must redirect a password-protected quarantine to the password-protected page" in {
        val fileUpload  = FileUpload(
          testReference,
          FileUploadStatus.UpscanQuarantine,
          failureMessage = Some("PUA.Doc.Packed.EncryptedDoc-6563700-0")
        )
        val application = applicationWithFileUpload(Future.successful(Some(fileUpload)))

        running(application) {
          val request = FakeRequest(GET, routes.FileProcessingController.onContinue(testReference).url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.FileUploadErrorController.filePasswordProtected().url
        }
      }

      "must redirect a virus quarantine to the virus page" in {
        val fileUpload  = FileUpload(
          testReference,
          FileUploadStatus.UpscanQuarantine,
          failureMessage = Some("Win.Test.EICAR_HDB-1")
        )
        val application = applicationWithFileUpload(Future.successful(Some(fileUpload)))

        running(application) {
          val request = FakeRequest(GET, routes.FileProcessingController.onContinue(testReference).url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.FileUploadErrorController.fileContainsVirus().url
        }
      }

      "must redirect to the generic upload failure page for a missing or unknown upload" in {
        Seq(None, Some(FileUpload(testReference, "UNKNOWN"))).foreach { fileUpload =>
          val application = applicationWithFileUpload(Future.successful(fileUpload))

          running(application) {
            val request = FakeRequest(GET, routes.FileProcessingController.onContinue(testReference).url)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.FileUploadErrorController.fileUploadFailed().url
          }
        }
      }

      "must redirect to the generic upload failure page when retrieving the upload fails" in {
        val application = applicationWithFileUpload(Future.failed(new RuntimeException("failed")))

        running(application) {
          val request = FakeRequest(GET, routes.FileProcessingController.onContinue(testReference).url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.FileUploadErrorController.fileUploadFailed().url
        }
      }
    }

    "status" - {

      "must return the server-selected redirect URL for a successful upload" in {

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
          contentAsJson(result) mustEqual Json.obj(
            "redirectUrl" -> routes.UploadedReportFilesController.onPageLoad().url
          )
        }
      }

      "must return the server-selected redirect URL for a structurally invalid file" in {

        val mockStorageService = mock[StorageService]

        when(
          mockStorageService.getFileUploadForThisPeriod(eqTo(testZReference), eqTo(testReference))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(
              Some(
                FileUpload(
                  reference = testReference,
                  status = "VALIDATION_FAILURE",
                  fileUploadDetails = Some(
                    FileUploadDetails(
                      fileName = "return.csv",
                      validation = Some(
                        ValidationResult(
                          rowsValidated = 0,
                          validationErrors = 0,
                          status = "InvalidFile",
                          invalidFileReason = Some("InvalidHeader")
                        )
                      )
                    )
                  )
                )
              )
            )
          )

        val application = applicationBuilder()
          .overrides(bind[StorageService].toInstance(mockStorageService))
          .build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.status(testReference).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.obj(
            "redirectUrl" -> routes.ProblemWithUploadedFileController.onPageLoad().url
          )
        }
      }

      "must use the generic route when row-level validation details are missing" in {

        val mockStorageService = mock[StorageService]

        when(
          mockStorageService.getFileUploadForThisPeriod(eqTo(testZReference), eqTo(testReference))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(
              Some(
                FileUpload(
                  reference = testReference,
                  status = "VALIDATION_FAILURE",
                  fileUploadDetails = Some(
                    FileUploadDetails(
                      fileName = "return.csv",
                      validation = Some(
                        ValidationResult(rowsValidated = 10, validationErrors = 3, status = "ValidationFailed")
                      )
                    )
                  )
                )
              )
            )
          )

        val application = applicationBuilder()
          .overrides(bind[StorageService].toInstance(mockStorageService))
          .build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.status(testReference).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.obj(
            "redirectUrl" -> routes.FileUploadErrorController.fileUploadFailed().url
          )
        }
      }

      "must return the password-protected redirect URL for an encrypted quarantine" in {

        val mockStorageService = mock[StorageService]

        when(
          mockStorageService.getFileUploadForThisPeriod(eqTo(testZReference), eqTo(testReference))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(
              Some(
                FileUpload(
                  reference = testReference,
                  status = "UPSCAN_QUARANTINE",
                  failureMessage = Some("PUA.Doc.Packed.EncryptedDoc-6563700-0")
                )
              )
            )
          )

        val application = applicationBuilder()
          .overrides(bind[StorageService].toInstance(mockStorageService))
          .build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.status(testReference).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.obj(
            "redirectUrl" -> routes.FileUploadErrorController.filePasswordProtected().url
          )
        }
      }

      "must return the virus redirect URL for a genuine virus quarantine" in {

        val mockStorageService = mock[StorageService]

        when(
          mockStorageService.getFileUploadForThisPeriod(eqTo(testZReference), eqTo(testReference))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(
              Some(
                FileUpload(
                  reference = testReference,
                  status = "UPSCAN_QUARANTINE",
                  failureMessage = Some("Win.Test.EICAR_HDB-1")
                )
              )
            )
          )

        val application = applicationBuilder()
          .overrides(bind[StorageService].toInstance(mockStorageService))
          .build()

        running(application) {

          val request =
            FakeRequest(GET, routes.FileProcessingController.status(testReference).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.obj(
            "redirectUrl" -> routes.FileUploadErrorController.fileContainsVirus().url
          )
        }
      }

      "must return the generic redirect URL when the file upload cannot be found" in {

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

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.obj(
            "redirectUrl" -> routes.FileUploadErrorController.fileUploadFailed().url
          )
        }
      }
    }
  }
}
