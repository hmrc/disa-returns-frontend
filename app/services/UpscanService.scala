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

package services

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.UpscanConnector
import models.upscan.{UpscanInitiateRequest, UpscanInitiateResponse}
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper

import scala.concurrent.Future

class UpscanService @Inject() (
  upscanConnector: UpscanConnector,
  dateHelper: DateHelper,
  appConfig: FrontendAppConfig
) {

  def initiate(zReference: String)(implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] =
    upscanConnector.initiateUpload(
      UpscanInitiateRequest(
        callbackUrl =
          s"${appConfig.disaReturnsBackendBaseUrl}/disa-returns-backend/monthly/upscan/callback/$zReference/${dateHelper.reportingPeriodTaxYear}/${dateHelper.reportingPeriodMonthNumber}",
        // Not built via the reverse router: UpscanService is unit-tested by constructing it directly,
        // without a running Application, and Play's reverse router resolves the context-path prefix
        // from process-global state that only a running Application sets up correctly - calling it
        // here would make the result depend on whichever other test suites happen to run concurrently
        // in the same JVM. These paths match controllers.routes.FileProcessingController.onPageLoad and
        // controllers.routes.UploadFileController.onError in conf/app.routes.
        successRedirect = Some(s"${appConfig.host}/file-processing"),
        errorRedirect = Some(s"${appConfig.host}/file-upload/error"),
        minimumFileSize = Some(appConfig.upscanMinFileSize),
        maximumFileSize = Some(appConfig.upscanMaxFileSize),
        expectedFileType = Some(appConfig.upscanAcceptedMimeTypes)
      )
    )
}
