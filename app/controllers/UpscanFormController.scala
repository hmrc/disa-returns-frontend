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
import forms.TestFormProvider
import models.UploadId
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import service.{UploadProgressTracker, UpscanService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{UploadFormView, UploadResult, UploadResultJs}
import play.api.libs.json.Json

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class UpscanFormController @Inject(
  upscanService: UpscanService,
  uploadFormView: UploadFormView,
  uploadResultView: UploadResult
) (
  val controllerComponents: MessagesControllerComponents,
  uploadProgressTracker: UploadProgressTracker
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {
  def onPageLoad(isaManagerReference: String): Action[AnyContent] = Action.async { implicit request =>
    val uploadId           = UploadId.generate()
    val successRedirectUrl = s"http://localhost:1205${routes.UpscanFormController.showResult(isaManagerReference).url}"
    for {
      upscanInitiateResponse <- upscanService.upscanInitiate(redirectOnSuccess = Some(successRedirectUrl), None)
      _                      <-
        uploadProgressTracker
          .requestUpload(uploadId, upscanInitiateResponse.fileReference.reference, isaManagerReference)

    } yield Ok(uploadFormView(upscanInitiateResponse))
  }

  def showResult(isaManagerReference: String): Action[AnyContent] = Action.async { implicit request =>

    uploadProgressTracker.getAllUploads(isaManagerReference).map {
      case statuses if statuses.nonEmpty =>
        Ok(uploadResultView(statuses, isaManagerReference))
      case _                             =>
        BadRequest(s"no results found for $isaManagerReference")
    }
  }

  def getStatuses(isaManagerReference: String): Action[AnyContent] = Action.async { implicit request =>
    uploadProgressTracker
      .getAllUploads(isaManagerReference)
      .map(statuses => Ok(Json.toJson(statuses)))
  }

}
