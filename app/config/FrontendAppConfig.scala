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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject(config: Configuration) extends ServicesConfig(config) {

  val host: String    = getString("host")
  val appName: String = getString("appName")

  lazy val disaReturnsBackendBaseUrl: String = baseUrl("disa-returns-backend")

  private val contactHost                  = getString("contact-frontend.host")
  private val contactFormServiceIdentifier = "disa-returns-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String         = getString("urls.login")
  val loginContinueUrl: String = getString("urls.loginContinue")
  val signOutUrl: String       = getString("urls.signOut")

  private val exitSurveyBaseUrl: String = baseUrl("feedback-frontend")
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/disa-returns-frontend"

  val languageTranslationEnabled: Boolean = getBoolean("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = getInt("timeout-dialog.timeout")
  val countdown: Int = getInt("timeout-dialog.countdown")

  lazy val upscanInitiateBase: String      = baseUrl("upscan-initiate")
  lazy val upscanMinFileSize: Int          = getInt("upscan.minFileSize")
  lazy val upscanMaxFileSize: Int          = getInt("upscan.maxFileSize")
  lazy val upscanAcceptedMimeTypes: String = getString("upscan.acceptedMimeTypes")

  val fileUploadMaxInlineErrors: Int = getInt("fileUploadValidation.maxInlineErrors")
}
