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

package navigation

import base.SpecBase
import controllers.routes
import models.*
import navigation.FileProcessingDecision.{Completed, Failed, Processing}
import play.api.mvc.Call
import play.api.test.Helpers.running

class FileUploadResultNavigatorSpec extends SpecBase {

  private val reference = "test-reference"

  private def upload(status: String): FileUpload = FileUpload(reference, status)

  private def validationFailure(
    status: String,
    validationErrors: Int = 0,
    inlineErrors: Seq[InlineError] = Seq.empty,
    invalidFileReason: Option[String] = None
  ): FileUpload =
    FileUpload(
      reference = reference,
      status = FileUploadStatus.ValidationFailure,
      fileUploadDetails = Some(
        FileUploadDetails(
          fileName = "return.csv",
          validation = Some(
            ValidationResult(
              rowsValidated = 1,
              validationErrors = validationErrors,
              status = status,
              inlineErrors = inlineErrors,
              invalidFileReason = invalidFileReason
            )
          )
        )
      )
    )

  "FileUploadResultNavigator" - {

    "must keep Created and Upscan success uploads processing" in {
      val application = applicationBuilder().build()

      running(application) {
        val navigator = application.injector.instanceOf[FileUploadResultNavigator]

        FileUploadStatus.pending.foreach { status =>
          navigator.getFileProcessingOutcome(Some(upload(status)), reference) mustEqual Processing
        }
      }
    }

    "must map every terminal upload outcome to one server-side destination" in {
      val application = applicationBuilder().build()

      running(application) {
        val navigator    = application.injector.instanceOf[FileUploadResultNavigator]
        val inlineErrors = Seq(InlineError(1, Seq("E010")))
        val cases        = Seq[(String, FileUpload, Call)](
          (
            "password protected",
            upload(FileUploadStatus.UpscanQuarantine).copy(failureMessage = Some("EncryptedDoc")),
            routes.FileUploadErrorController.filePasswordProtected()
          ),
          (
            "virus quarantine",
            upload(FileUploadStatus.UpscanQuarantine).copy(failureMessage = Some("Eicar")),
            routes.FileUploadErrorController.fileContainsVirus()
          ),
          (
            "rejected type",
            upload(FileUploadStatus.UpscanRejected),
            routes.FileUploadErrorController.invalidFileType()
          ),
          (
            "duplicate",
            upload(FileUploadStatus.Duplicate),
            routes.FileUploadErrorController.duplicateFileUpload()
          ),
          (
            "successful validation",
            upload(FileUploadStatus.ValidationSuccess),
            routes.UploadedReportFilesController.onPageLoad()
          ),
          (
            "invalid header",
            validationFailure(
              FileUploadValidationStatus.InvalidFile,
              invalidFileReason = Some(InvalidFileReason.InvalidHeader)
            ),
            routes.ProblemWithUploadedFileController.onPageLoad()
          ),
          (
            "invalid workbook",
            validationFailure(
              FileUploadValidationStatus.InvalidFile,
              invalidFileReason = Some(InvalidFileReason.InvalidWorkbook)
            ),
            routes.ProblemWithUploadedFileController.onPageLoad()
          ),
          (
            "invalid file",
            validationFailure(
              FileUploadValidationStatus.InvalidFile,
              invalidFileReason = Some(InvalidFileReason.InvalidFile)
            ),
            routes.ProblemWithUploadedFileController.onPageLoad()
          ),
          (
            "no data rows",
            validationFailure(
              FileUploadValidationStatus.InvalidFile,
              invalidFileReason = Some(InvalidFileReason.NoDataRows)
            ),
            routes.FileUploadErrorController.emptyFileUploaded()
          ),
          (
            "unsupported validation type",
            validationFailure(
              FileUploadValidationStatus.InvalidFile,
              invalidFileReason = Some(InvalidFileReason.UnsupportedFileType)
            ),
            routes.FileUploadErrorController.invalidFileType()
          ),
          (
            "up to 25 row errors",
            validationFailure(
              FileUploadValidationStatus.ValidationFailed,
              validationErrors = 25,
              inlineErrors = inlineErrors
            ),
            routes.FileValidationErrorsController.onPageLoad(reference)
          ),
          (
            "more than 25 row errors",
            validationFailure(
              FileUploadValidationStatus.ValidationFailed,
              validationErrors = 26,
              inlineErrors = inlineErrors
            ),
            routes.FileFormattingErrorsController.onPageLoad()
          ),
          (
            "unknown Upscan outcome",
            upload(FileUploadStatus.UpscanUnknown),
            routes.FileUploadErrorController.fileUploadFailed()
          ),
          (
            "expired Upscan outcome",
            upload(FileUploadStatus.UpscanExpired),
            routes.FileUploadErrorController.fileUploadFailed()
          )
        )

        cases.foreach { case (_, fileUpload, expectedCall) =>
          navigator.getFileProcessingOutcome(Some(fileUpload), reference) mustEqual Completed(expectedCall)
        }
      }
    }

    "must use the generic technical route for missing, malformed and unknown outcomes" in {
      val application = applicationBuilder().build()

      running(application) {
        val appConfig      = application.injector.instanceOf[config.FrontendAppConfig]
        val originalPrefix = app.RoutesPrefix.prefix

        try {
          app.RoutesPrefix.setPrefix("/before-mount")
          val navigator = new FileUploadResultNavigator(appConfig)

          app.RoutesPrefix.setPrefix("/after-mount")
          val generic = routes.FileUploadErrorController.fileUploadFailed()

          navigator.getFileProcessingOutcome(None, reference) mustEqual Failed(generic)
          navigator.getFileProcessingOutcome(Some(upload("UNKNOWN")), reference) mustEqual Failed(generic)
          navigator.getFileProcessingOutcome(
            Some(upload(FileUploadStatus.ValidationFailure)),
            reference
          ) mustEqual Completed(generic)
          navigator.getFileProcessingOutcome(
            Some(validationFailure(FileUploadValidationStatus.ValidationFailed, validationErrors = 3)),
            reference
          ) mustEqual Completed(generic)
          navigator.getFileProcessingOutcome(
            Some(validationFailure(FileUploadValidationStatus.InvalidFile, invalidFileReason = Some("UnknownReason"))),
            reference
          ) mustEqual Completed(generic)
        } finally
          app.RoutesPrefix.setPrefix(originalPrefix)
      }
    }
  }
}
