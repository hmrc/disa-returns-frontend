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
import handlers.ErrorHandler
import models.MonthlyReturnDeclarationResult.{Declared, Failed}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StorageService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.DeclarationViewModel
import views.html.DeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  storageService: StorageService,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: DeclarationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      Ok(view(DeclarationViewModel(request.monthlyReturn.nilReturn)))
    }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    storageService
      .declareForThisWindow(request.zReference)
      .flatMap {
        case Declared => Future.successful(Redirect(routes.SubmissionCompleteController.onPageLoad()))
        case Failed   => errorHandler.internalServerError
      }
  }
}
