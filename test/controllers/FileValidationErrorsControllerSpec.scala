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
import models._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.FileValidationErrorsView

class FileValidationErrorsControllerSpec extends SpecBase {

  private val testReference = "test-file-reference"

  private def fileUploadWithErrors(inlineErrors: Seq[InlineError]): FileUpload =
    FileUpload(
      reference = testReference,
      status = FileUploadStatus.ValidationSuccess,
      fileUploadDetails = Some(
        FileUploadDetails(
          fileName = "return.csv",
          validation = Some(
            ValidationResult(
              rowsValidated = inlineErrors.size,
              validationErrors = inlineErrors.flatMap(_.errorCodes).size,
              status = "FAILURE",
              inlineErrors = inlineErrors
            )
          )
        )
      )
    )

  private val twentyFiveInlineErrors: Seq[InlineError] = Seq(
    InlineError(rowNumber = 1, errorCodes = Seq("E010", "E020", "E030", "E040", "E050")),
    InlineError(rowNumber = 2, errorCodes = Seq("E060", "E070", "E080", "E090", "E100")),
    InlineError(rowNumber = 3, errorCodes = Seq("E110", "E120", "E130", "E140", "E150")),
    InlineError(rowNumber = 4, errorCodes = Seq("E160", "E170", "E180", "E190", "E011")),
    InlineError(rowNumber = 5, errorCodes = Seq("E020", "E021", "E022", "E023", "E024"))
  )

  private val twentySixInlineErrors: Seq[InlineError] =
    twentyFiveInlineErrors :+ InlineError(rowNumber = 6, errorCodes = Seq("E010"))

  "FileValidationErrorsController" - {

    "must return OK and the correct view when there are 25 or fewer errors" in {
      val monthlyReturn = emptyMonthlyReturn.copy(fileUploads = Seq(fileUploadWithErrors(twentyFiveInlineErrors)))
      val application   = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.FileValidationErrorsController.onPageLoad(testReference).url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[FileValidationErrorsView]

        val expectedErrors = twentyFiveInlineErrors.flatMap { inlineError =>
          inlineError.errorCodes.map { code =>
            FileValidationError(
              cell = FileValidationErrorCodes.cellReference(code, inlineError.rowNumber),
              messageKey = FileValidationErrorCodes.messageKey(code)
            )
          }
        }

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedErrors)(request, messages(application)).toString
      }
    }

    "must redirect to ProblemWithUploadedFileController when any inline error contains E001" in {
      val inlineErrors  = Seq(
        InlineError(rowNumber = 1, errorCodes = Seq("E001", "E010")),
        InlineError(rowNumber = 2, errorCodes = Seq("E020"))
      )
      val monthlyReturn = emptyMonthlyReturn.copy(fileUploads = Seq(fileUploadWithErrors(inlineErrors)))
      val application   = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.FileValidationErrorsController.onPageLoad(testReference).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ProblemWithUploadedFileController.onPageLoad().url
      }
    }

    "must redirect to FileFormattingErrorsController when there are more than 25 errors" in {
      val monthlyReturn = emptyMonthlyReturn.copy(fileUploads = Seq(fileUploadWithErrors(twentySixInlineErrors)))
      val application   = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.FileValidationErrorsController.onPageLoad(testReference).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.FileFormattingErrorsController.onPageLoad().url
      }
    }

    "must return OK with an empty error table when the file reference is not found" in {
      val application = applicationBuilder(monthlyReturn = Some(emptyMonthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.FileValidationErrorsController.onPageLoad("unknown-reference").url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[FileValidationErrorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Seq.empty)(request, messages(application)).toString
      }
    }

    "must return OK with an empty error table when the file has no validation results" in {
      val uploadWithoutValidation = FileUpload(
        reference = testReference,
        status = FileUploadStatus.UpscanSuccess,
        fileUploadDetails = Some(FileUploadDetails("return.csv"))
      )
      val monthlyReturn           = emptyMonthlyReturn.copy(fileUploads = Seq(uploadWithoutValidation))
      val application             = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.FileValidationErrorsController.onPageLoad(testReference).url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[FileValidationErrorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Seq.empty)(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecoveryController when no monthly return is found" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(GET, routes.FileValidationErrorsController.onPageLoad(testReference).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
