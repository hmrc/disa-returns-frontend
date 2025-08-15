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
import jakarta.inject.Singleton
import models.{UploadId, UploadStatus}
import models.upscan.{CallbackBody, FailedCallbackBody, ReadyCallbackBody}
import play.api.i18n.I18nSupport
import play.api.libs.json.JsValue
import play.api.mvc.{Action, MessagesControllerComponents}
import service.UploadProgressTracker
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanCallbackController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  uploadProgressTracker: UploadProgressTracker
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {
  def onPageLoad(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[CallbackBody] { callbackBody =>
      UpscanCallbackHandler(callbackBody).map(_ => Ok )
    }
  }

  private def UpscanCallbackHandler(callbackBody: CallbackBody)(implicit hc: HeaderCarrier): Future[Unit] = {
    val uploadStatus = callbackBody match {
      case s: ReadyCallbackBody  =>
        UploadStatus.UploadedSuccessfully(
          name = s.uploadDetails.fileName,
          mimeType = s.uploadDetails.fileMimeType,
          downloadUrl = s.downloadUrl,
          size = Some(s.uploadDetails.size),
          checksum = s.uploadDetails.checksum
        )
      case f: FailedCallbackBody =>
        UploadStatus.Failed
    }
    uploadProgressTracker.registerUploadResult(callbackBody.reference, uploadStatus)
  }
}
