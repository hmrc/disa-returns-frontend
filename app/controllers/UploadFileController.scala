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

import controllers.actions.IdentifierAction
import handlers.ErrorHandler
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import service.UpscanService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UploadFileView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class UploadFileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  upscanService: UpscanService,
  val controllerComponents: MessagesControllerComponents,
  view: UploadFileView,
  errorHandler: ErrorHandler
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    identify.async { implicit request =>
      upscanService
        .initiate()
        .map { upscanResponse =>
          Ok(view(upscanResponse))
        }
        .recoverWith { case NonFatal(ex) =>
          logger.error(
            s"[UploadFileController][onPageLoad] Request to upscan initiate failed with exception: [$ex]"
          )
          errorHandler.internalServerError(request)
        }
    }
}
