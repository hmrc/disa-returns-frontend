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
          s"${appConfig.disaReturnsBackendBaseUrl}/disa-returns-backend/monthly/upscan/callback/$zReference/${dateHelper.taxYear}/${dateHelper.month}",
        successRedirect = Some(s"${appConfig.host}/upscan/success"),
        errorRedirect = Some(s"${appConfig.host}/file-upload"),
        minimumFileSize = Some(appConfig.upscanMinFileSize),
        maximumFileSize = Some(appConfig.upscanMaxFileSize),
        expectedFileType = Some(appConfig.upscanAcceptedMimeTypes)
      )
    )
}
