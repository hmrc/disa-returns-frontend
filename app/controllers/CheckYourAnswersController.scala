/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.DataRequest
import navigation.Navigator
import pages.CheckYourAnswersPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: Navigator,
  dateHelper: DateHelper,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Ok(view(summaryList))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(CheckYourAnswersPage, request.monthlyReturn))
  }

  private def summaryList(implicit request: DataRequest[_], messages: Messages): SummaryList = {
    val monthlyReturn = request.monthlyReturn

    SummaryListViewModel(
      Seq(reportSubmissionRow(monthlyReturn.nilReturn)) ++ fileRows
    )
  }

  private def reportSubmissionRow(nilReturn: Boolean)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = KeyViewModel(Text(messages("monthlyReportSubmission.question", dateHelper.reportingWindowMonth))),
      value = ValueViewModel(Text(reportSubmissionAnswer(nilReturn))),
      actions = Seq(
        changeAction(
          routes.MonthlyReportSubmissionController.onPageLoad().url,
          messages("checkYourAnswers.reportSubmission.change.hidden")
        )
      )
    )

  private def fileRows(implicit request: DataRequest[_], messages: Messages): Seq[SummaryListRow] =
    if (request.monthlyReturn.nilReturn) {
      Seq.empty
    } else {
      Seq(
        SummaryListRowViewModel(
          key = KeyViewModel(Text(messages("checkYourAnswers.files"))),
          value = ValueViewModel(fileNamesContent),
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

  private def fileNamesContent(implicit request: DataRequest[_]): HtmlContent = {
    val fileNames = request.monthlyReturn.fileUploads
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
