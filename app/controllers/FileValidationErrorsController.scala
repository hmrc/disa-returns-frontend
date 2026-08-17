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

import com.google.inject.Inject
import controllers.actions.JourneyGuard.Page
import controllers.actions.{DataRetrievalAction, IdentifierAction, JourneyGuard}
import handlers.ErrorHandler
import models.{FileValidationError, FileValidationErrorCodes, InlineError}
import navigation.FileProcessingDecision.{Completed, Failed, Processing}
import navigation.FileUploadResultNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.FileValidationErrorsView

import scala.concurrent.Future

class FileValidationErrorsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  journeyGuard: JourneyGuard,
  val controllerComponents: MessagesControllerComponents,
  view: FileValidationErrorsView,
  resultNavigator: FileUploadResultNavigator,
  errorHandler: ErrorHandler
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(reference: String): Action[AnyContent] =
    (identify andThen getData andThen journeyGuard(Page.FileValidationErrors)).async { implicit request =>
      val fileUpload = request.monthlyReturn.fileUploads
        .find(_.reference == reference)

      resultNavigator.getFileProcessingOutcome(fileUpload, reference) match {
        case Completed(destination) if destination == routes.FileValidationErrorsController.onPageLoad(reference) =>
          val inlineErrors = fileUpload.toSeq
            .flatMap(_.fileUploadDetails)
            .flatMap(_.validation)
            .flatMap(_.inlineErrors)

          Future.successful(Ok(view(toFileValidationErrors(inlineErrors))))
        case Completed(destination)                                                                               =>
          Future.successful(Redirect(destination))
        case Failed(destination)                                                                                  =>
          Future.successful(Redirect(destination))
        case Processing                                                                                           =>
          genericFailureResult(reference)
      }
    }

  private def genericFailureResult(reference: String)(implicit request: RequestHeader): Future[Result] =
    resultNavigator.getFileProcessingOutcome(None, reference) match {
      case Failed(destination) => Future.successful(Redirect(destination))
      case _                   => errorHandler.internalServerError
    }

  private def toFileValidationErrors(inlineErrors: Seq[InlineError]): Seq[FileValidationError] =
    inlineErrors.flatMap { inlineError =>
      inlineError.errorCodes.map { code =>
        FileValidationError(
          cell = FileValidationErrorCodes.cellReference(code, inlineError.rowNumber),
          messageKey = FileValidationErrorCodes.messageKey(code)
        )
      }
    }
}
