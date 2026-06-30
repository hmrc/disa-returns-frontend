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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{FileValidationError, FileValidationErrorCodes, InlineError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.FileValidationErrorsView

class FileValidationErrorsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: FileValidationErrorsView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(reference: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val inlineErrors = request.monthlyReturn.fileUploads
        .find(_.reference == reference)
        .flatMap(_.fileUploadDetails)
        .flatMap(_.validation)
        .toSeq
        .flatMap(_.inlineErrors)

      if (inlineErrors.exists(_.errorCodes.contains("E001")))
        Redirect(routes.ProblemWithUploadedFileController.onPageLoad())
      else {
        val errors = toFileValidationErrors(inlineErrors)

        if (errors.size > 25)
          Redirect(routes.FileFormattingErrorsController.onPageLoad())
        else
          Ok(view(errors))
      }
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
