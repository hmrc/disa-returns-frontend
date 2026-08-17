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
import controllers.actions.IdentifierAction
import play.api.{Environment, Logging}
import play.api.http.HeaderNames.CONTENT_DISPOSITION
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DownloadReportTemplateView

import javax.inject.Inject
import scala.util.Using

class DownloadReportTemplateController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: DownloadReportTemplateView,
  environment: Environment,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val excelFilename = "^[A-Za-z0-9][A-Za-z0-9._ -]*\\.xlsx$".r

  def onPageLoad(): Action[AnyContent] = identify { implicit request =>
    Ok(view())
  }

  def download(): Action[AnyContent] = identify {
    (appConfig.monthlyReportTemplateResourcePath, appConfig.monthlyReportTemplateFilename) match {
      case (Some(resourcePath), Some(filename @ excelFilename())) =>
        environment.resourceAsStream(resourcePath) match {
          case Some(stream) =>
            val bytes = Using.resource(stream)(_.readAllBytes())

            Ok(bytes)
              .as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
              .withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=\"$filename\"")
          case None         =>
            logger.error(s"Monthly report template resource [$resourcePath] is not available")
            ServiceUnavailable
        }
      case _                                                      =>
        logger.error("Monthly report template resource path and filename have not been configured")
        ServiceUnavailable
    }
  }
}
