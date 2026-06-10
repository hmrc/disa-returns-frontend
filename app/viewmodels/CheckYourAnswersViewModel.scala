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

import controllers.routes
import models.MonthlyReturn
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*

final case class CheckYourAnswersViewModel(
  summaryList: SummaryList
)

object CheckYourAnswersViewModel {

  def apply(monthlyReturn: MonthlyReturn, reportingWindowMonth: String)(implicit
    messages: Messages
  ): CheckYourAnswersViewModel = {
    val rows = Seq(reportSubmissionRow(monthlyReturn.nilReturn, reportingWindowMonth)) ++ fileRows(monthlyReturn)

    CheckYourAnswersViewModel(
      summaryList = SummaryListViewModel(rows)
    )
  }

  private def reportSubmissionRow(nilReturn: Boolean, reportingWindowMonth: String)(implicit
    messages: Messages
  ): SummaryListRow =
    SummaryListRowViewModel(
      key = KeyViewModel(Text(messages("monthlyReportSubmission.question", reportingWindowMonth))),
      value = ValueViewModel(Text(reportSubmissionAnswer(nilReturn))),
      actions = Seq(
        changeAction(
          routes.MonthlyReportSubmissionController.onPageLoad().url,
          reportSubmissionAnswer(nilReturn)
        )
      )
    )

  private def fileRows(monthlyReturn: MonthlyReturn)(implicit messages: Messages): Seq[SummaryListRow] =
    if (monthlyReturn.nilReturn) {
      Seq.empty
    } else {
      Seq(
        SummaryListRowViewModel(
          key = KeyViewModel(Text(messages("checkYourAnswers.files"))),
          value = ValueViewModel(fileNamesContent(monthlyReturn)),
          actions = Seq(
            changeAction(
              routes.UploadedReportFilesController.onPageLoad().url,
              messages("checkYourAnswers.files.change.hidden")
            )
          )
        )
      )
    }

  private def reportSubmissionAnswer(nilReturn: Boolean)(implicit messages: Messages): String =
    if (nilReturn) {
      messages("monthlyReportSubmission.no")
    } else {
      messages("monthlyReportSubmission.yes")
    }

  private def fileNamesContent(monthlyReturn: MonthlyReturn): HtmlContent = {
    val fileNames = monthlyReturn.fileUploads
      .filter(_.isSuccessful)
      .flatMap(_.fileUploadDetails)
      .map(details => HtmlFormat.escape(details.fileName).toString)

    HtmlContent(fileNames.mkString("<br>"))
  }

  private def changeAction(href: String, visuallyHiddenText: String)(implicit messages: Messages): ActionItem =
    ActionItemViewModel(
      content = Text(messages("site.change")),
      href = href
    ).withVisuallyHiddenText(visuallyHiddenText)
}
