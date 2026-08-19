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
import navigation.FileProcessingDecision.{Completed, Failed, Processing}
import navigation.FileUploadResultNavigator
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.StorageService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{FileProcessingProgressView, FileProcessingView}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class FileProcessingController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  storageService: StorageService,
  resultNavigator: FileUploadResultNavigator,
  val controllerComponents: MessagesControllerComponents,
  view: FileProcessingView,
  progressView: FileProcessingProgressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(key: Option[String]): Action[AnyContent] = identify { implicit request =>
    key match {
      case Some(reference) => Ok(view(reference = reference))
      case None            => Redirect(routes.FileUploadErrorController.fileUploadFailed())
    }
  }

  def checkProgress(reference: String): Action[AnyContent] = identify.async { implicit request =>
    storageService
      .getFileUploadForThisPeriod(request.zReference, reference)
      .map(resultNavigator.getFileProcessingOutcome(_, reference))
      .recover { case NonFatal(ex) =>
        logger.error(s"[FileProcessingController][checkProgress] Failed to retrieve upload $reference", ex)
        resultNavigator.getFileProcessingOutcome(None, reference)
      }
      .map {
        case Processing          => Ok(progressView(reference, complete = false))
        case Completed(_)        => Ok(progressView(reference, complete = true))
        case Failed(destination) => Redirect(destination)
      }
  }

  def onContinue(reference: String): Action[AnyContent] = identify.async { implicit request =>
    storageService
      .getFileUploadForThisPeriod(request.zReference, reference)
      .map(resultNavigator.getFileProcessingOutcome(_, reference))
      .recover { case NonFatal(ex) =>
        logger.error(s"[FileProcessingController][onContinue] Failed to retrieve upload $reference", ex)
        resultNavigator.getFileProcessingOutcome(None, reference)
      }
      .map {
        case Processing             => Ok(progressView(reference, complete = false))
        case Completed(destination) => Redirect(destination)
        case Failed(destination)    => Redirect(destination)
      }
  }

  def status(reference: String): Action[AnyContent] = identify.async { implicit request =>
    storageService
      .getFileUploadForThisPeriod(request.zReference, reference)
      .map(resultNavigator.getFileProcessingOutcome(_, reference))
      .recover { case NonFatal(ex) =>
        logger.error(s"[FileProcessingController][status] Failed to retrieve upload $reference", ex)
        resultNavigator.getFileProcessingOutcome(None, reference)
      }
      .map(statusResult)
  }

  private def statusResult(decision: navigation.FileProcessingDecision): Result =
    decision match {
      case Processing             => Ok(Json.obj("processing" -> true))
      case Completed(destination) => Ok(Json.obj("redirectUrl" -> destination.url))
      case Failed(destination)    => Ok(Json.obj("redirectUrl" -> destination.url))
    }
}
