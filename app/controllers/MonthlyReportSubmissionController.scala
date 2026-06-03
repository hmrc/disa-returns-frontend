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

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import forms.MonthlyReportSubmissionFormProvider
import models.YesNoAnswer.{No, Yes}
import models.{MonthlyReturnSubmission, NormalMode}
import navigation.Navigator
import pages.MonthlyReportSubmissionPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StorageService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UuidGenerator
import views.html.MonthlyReportSubmissionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MonthlyReportSubmissionController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: MonthlyReportSubmissionFormProvider,
  navigator: Navigator,
  storageService: StorageService,
  uuidGenerator: UuidGenerator,
  val controllerComponents: MessagesControllerComponents,
  view: MonthlyReportSubmissionView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val preparedForm = request.monthlyReturnSubmission
      .map(submission => if (submission.nilReport) form.fill(No) else form.fill(Yes))
      .getOrElse(form)

    Future.successful(Ok(view(preparedForm)))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors))
          ),
        answer => {
          val submission = request.monthlyReturnSubmission.fold {
            MonthlyReturnSubmission(uuidGenerator.generate(), nilReport = answer == No)
          } {
            _.copy(nilReport = answer == No)
          }

          storageService
            .upsertForThisWindow(request.zReference, submission)
            .map { upsertedSubmission =>
              Redirect(navigator.nextPage(MonthlyReportSubmissionPage, NormalMode, upsertedSubmission))
            }
            .recover { case NonFatal(e) =>
              logger.error(
                s"Failed to save monthly return submission for zRef: [${request.zReference}]",
                e
              )
              InternalServerError
            }
        }
      )
  }
}
