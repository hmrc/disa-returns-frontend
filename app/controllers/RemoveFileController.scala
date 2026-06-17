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
import forms.RemoveFileFormProvider
import models.YesNoAnswer
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StorageService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemoveFileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RemoveFileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemoveFileFormProvider,
  storageService: StorageService,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveFileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def fileNameFor(reference: String)(implicit request: models.requests.DataRequest[?]): Option[String] =
    request.monthlyReturn.fileUploads
      .find(f => f.reference == reference && f.isSuccessful)
      .flatMap(_.fileUploadDetails)
      .map(_.fileName)

  def onPageLoad(reference: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      fileNameFor(reference) match {
        case Some(fileName) => Ok(view(formProvider(fileName), reference, fileName))
        case None           => Redirect(routes.UploadedReportFilesController.onPageLoad())
      }
  }

  def onSubmit(reference: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      fileNameFor(reference) match {
        case None           => Future.successful(Redirect(routes.UploadedReportFilesController.onPageLoad()))
        case Some(fileName) =>
          formProvider(fileName)
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, reference, fileName))),
              {
                case YesNoAnswer.Yes =>
                  val remainingSuccessfulFiles = request.monthlyReturn.fileUploads
                    .filter(f => f.isSuccessful && f.reference != reference)
                  storageService
                    .deleteFileUploadForThisWindow(request.zReference, reference)
                    .map { _ =>
                      if (remainingSuccessfulFiles.isEmpty)
                        Redirect(routes.UploadFileController.onPageLoad())
                      else
                        Redirect(routes.UploadedReportFilesController.onPageLoad())
                    }
                    .recover { case NonFatal(e) =>
                      logger.error(
                        s"Failed to delete file upload for reference: [$reference] and zRef: [${request.zReference}]",
                        e
                      )
                      InternalServerError
                    }
                case YesNoAnswer.No  =>
                  Future.successful(Redirect(routes.UploadedReportFilesController.onPageLoad()))
              }
            )
      }
  }
}
