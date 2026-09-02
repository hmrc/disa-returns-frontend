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

import config.FrontendAppConfig
import controllers.actions.JourneyGuard.Page
import controllers.actions.{DataRetrievalAction, IdentifierAction, JourneyGuard}
import handlers.ErrorHandler
import models.upscan.UploadError
import play.api.http.Status.UNPROCESSABLE_ENTITY
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{StorageService, UpscanService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.http.UpstreamErrorResponse
import viewmodels.UploadViewModel
import views.html.UploadFileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UploadFileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  upscanService: UpscanService,
  storageService: StorageService,
  val controllerComponents: MessagesControllerComponents,
  view: UploadFileView,
  errorHandler: ErrorHandler,
  getData: DataRetrievalAction,
  journeyGuard: JourneyGuard,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onError(): Action[AnyContent] = Action { implicit request =>
    val errorCode = request.getQueryString("errorCode").getOrElse("failed")
    Redirect(routes.UploadFileController.onPageLoad().url, Map("errorCode" -> Seq(errorCode)))
  }

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen journeyGuard(Page.UploadFile)).async { implicit request =>
      val error: Option[String] =
        request.getQueryString("errorCode").map(UploadError.toMessageKey)

      upscanService
        .initiate(request.zReference, request.currentDate)
        .flatMap { upscanResponse =>
          storageService
            .createFileUploadForThisPeriod(request.zReference, upscanResponse.reference, request.currentDate)
            .map { _ =>
              val model = UploadViewModel(
                upscan = upscanResponse,
                error = error
              )
              Ok(view(model))
            }
        }
        .recoverWith {
          case ex: UpstreamErrorResponse if ex.statusCode == UNPROCESSABLE_ENTITY =>
            Future.successful(Redirect(appConfig.manageIsasUrl))
          case NonFatal(ex)                                                       =>
            logger.error(s"[UploadFileController] initiate failed: $ex")
            errorHandler.internalServerError(request)
        }
    }
}
