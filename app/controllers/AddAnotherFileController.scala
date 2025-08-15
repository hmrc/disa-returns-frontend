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
import models.{UploadId, UploadStatus}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import service.{UploadProgressTracker, UpscanService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{UploadFormView, UploadResult}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddAnotherFileController @Inject(
  upscanService: UpscanService,
  uploadFormView: UploadFormView,
  uploadResultView: UploadResult,
  formProvider: TestFormProvider
) (
  val controllerComponents: MessagesControllerComponents,
  uploadProgressTracker: UploadProgressTracker
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {
  
  def onSubmit(isaManagerReference: String): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Redirect(routes.UpscanFormController.onPageLoad(isaManagerReference))
    )
  }
}
