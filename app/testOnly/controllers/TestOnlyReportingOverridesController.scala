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

package testOnly.controllers

import handlers.ErrorHandler
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader, Result}
import controllers.actions.IdentifierAction
import testOnly.connectors.TestOnlyReportingOverridesConnector
import testOnly.forms.{TestOnlyReportingOverrides, TestOnlyReportingOverridesFormProvider}
import testOnly.views.html.TestOnlyReportingOverridesView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TestOnlyReportingOverridesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  formProvider: TestOnlyReportingOverridesFormProvider,
  connector: TestOnlyReportingOverridesConnector,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyReportingOverridesView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = Action.async { implicit request =>
    identify.invokeBlockWithoutReportingContext(request, zReference => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      connector
        .get(zReference)
        .map { current =>
          val form = current.reportingWindow match {
            case Some((start, end)) => formProvider().fill(TestOnlyReportingOverrides(start, end, current.systemDate))
            case None               =>
              val systemDateData = current.systemDate.fold(Map.empty[String, String]) { date =>
                Map(
                  "systemDate.day"   -> date.getDayOfMonth.toString,
                  "systemDate.month" -> date.getMonthValue.toString,
                  "systemDate.year"  -> date.getYear.toString
                )
              }
              formProvider().bind(systemDateData).discardingErrors
          }
          Ok(view(form))
        }
        .recoverWith(logAndHandle("load", zReference))
    })
  }

  def onSubmit(): Action[AnyContent] = Action.async { implicit request =>
    identify.invokeBlockWithoutReportingContext(request, zReference =>
      formProvider()
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(view(errors))),
          overrides => {
            implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
            connector
              .set(zReference, overrides)
              .map(_ => Redirect(testOnly.controllers.routes.TestOnlyReportingOverridesController.onPageLoad()))
              .recoverWith(logAndHandle("set", zReference))
          }
        )
    )
  }

  def reset(): Action[AnyContent] = Action.async { implicit request =>
    identify.invokeBlockWithoutReportingContext(request, zReference => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      connector
        .reset(zReference)
        .map(_ => Redirect(testOnly.controllers.routes.TestOnlyReportingOverridesController.onPageLoad()))
        .recoverWith(logAndHandle("reset", zReference))
    })
  }

  private def logAndHandle(operation: String, zReference: String)(implicit
    request: RequestHeader
  ): PartialFunction[Throwable, Future[Result]] = {
    case NonFatal(exception) =>
      logger.error(s"Failed to $operation test-only reporting overrides for zReference [$zReference]", exception)
      errorHandler.internalServerError
  }
}
