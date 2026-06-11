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

package viewmodels

import base.SpecBase
import controllers.routes
import models.{FileUpload, FileUploadDetails, FileUploadStatus, MonthlyReturn}
import play.api.i18n.Messages
import play.api.test.Helpers.running
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

class CheckYourAnswersViewModelSpec extends SpecBase {

  private val successfulUploadOne = FileUpload(
    reference = "successful-reference-1",
    status = FileUploadStatus.UpscanSuccess,
    fileUploadDetails = Some(FileUploadDetails("file1.csv"))
  )

  private val successfulUploadTwo = FileUpload(
    reference = "successful-reference-2",
    status = FileUploadStatus.UpscanSuccess,
    fileUploadDetails = Some(FileUploadDetails("file2.csv"))
  )

  private val inProgressUpload = FileUpload(
    reference = "in-progress-reference",
    status = "CREATED"
  )

  "CheckYourAnswersViewModel" - {

    "must build a summary list for a nil return" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val viewModel = CheckYourAnswersViewModel(
          emptyMonthlyReturn.copy(nilReturn = true),
          testReportingWindowMonthName
        )

        val rows = viewModel.summaryList.rows

        rows.size mustEqual 1
        rows.head.key.content mustEqual Text("Are you submitting a report file for March window?")
        rows.head.value.content mustEqual Text("No - I have a nil report")
        rows.head.actions.value.items.head.href mustEqual routes.MonthlyReportSubmissionController.onPageLoad().url
      }
    }

    "must build a summary list for a report submission" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val monthlyReturn = MonthlyReturn(
          submissionId = testSubmissionId,
          nilReturn = false,
          fileUploads = Seq(successfulUploadOne, inProgressUpload, successfulUploadTwo)
        )

        val viewModel = CheckYourAnswersViewModel(monthlyReturn, testReportingWindowMonthName)

        val rows = viewModel.summaryList.rows

        rows.size mustEqual 2
        rows.head.key.content mustEqual Text("Are you submitting a report file for March window?")
        rows.head.value.content mustEqual Text("Yes - I am uploading a report")
        rows.head.actions.value.items.head.href mustEqual routes.MonthlyReportSubmissionController.onPageLoad().url

        rows(1).key.content mustEqual Text("Files")
        rows(1).value.content mustEqual HtmlContent("file1.csv,<br>file2.csv")
        rows(1).actions.value.items.head.href mustEqual routes.UploadedReportFilesController.onPageLoad().url
      }
    }
  }
}
