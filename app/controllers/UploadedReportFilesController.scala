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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.UploadedReportFilesFormProvider
import models.NormalMode
import navigation.Navigator
import pages.UploadedReportFilesPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UploadedReportFilesView

import javax.inject.Inject
import scala.concurrent.Future

class UploadedReportFilesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: UploadedReportFilesFormProvider,
  navigator: Navigator,
  val controllerComponents: MessagesControllerComponents,
  view: UploadedReportFilesView
) extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val uploadedReportFiles = request.monthlyReturn.fileUploads.filter(_.isSuccessful)

    Ok(view(form, uploadedReportFiles))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val uploadedReportFiles = request.monthlyReturn.fileUploads.filter(_.isSuccessful)

    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, uploadedReportFiles))
          ),
        answer =>
          Future.successful(
            Redirect(navigator.nextPage(UploadedReportFilesPage, NormalMode, answer))
          )
      )
  }
}
