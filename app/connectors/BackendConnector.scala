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

package connectors

import config.FrontendAppConfig
import models.Month.Month
import models.MonthlyReturnSubmission
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BackendConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) {

  private val backendUrl =
    s"${appConfig.disaReturnsBackendBaseUrl}/disa-returns-backend/monthly-return-submissions"

  def retrieve(zRef: String, taxYear: String, submissionPeriod: Month)(implicit
    hc: HeaderCarrier
  ): Future[Option[MonthlyReturnSubmission]] = {
    val period = submissionPeriod.toString

    httpClient
      .get(url"$backendUrl/$zRef/$taxYear/$period")
      .execute[Option[MonthlyReturnSubmission]]
  }

  def upsert(submission: MonthlyReturnSubmission, zRef: String, taxYear: String, submissionPeriod: Month)(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturnSubmission] = {
    val period = submissionPeriod.toString

    httpClient
      .put(url"$backendUrl/$zRef/$taxYear/$period")
      .withBody(Json.toJson(submission))
      .execute[MonthlyReturnSubmission]
  }
}
