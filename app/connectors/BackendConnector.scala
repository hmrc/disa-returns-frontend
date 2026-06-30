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
import models.{CreateMonthlyReturnResponse, MonthlyReturn}
import play.api.http.Status.{CREATED, NO_CONTENT}
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BackendConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) {

  private val backendUrl =
    s"${appConfig.disaReturnsBackendBaseUrl}/disa-returns-backend/monthly"

  def retrieve(zRef: String, taxYear: String, month: Int)(implicit
    hc: HeaderCarrier
  ): Future[Option[MonthlyReturn]] =
    httpClient
      .get(url"$backendUrl/$zRef/$taxYear/$month")
      .execute[Option[MonthlyReturn]]

  def createMonthlyReturn(nilReturn: Boolean, zRef: String, taxYear: String, month: Int)(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturn] =
    httpClient
      .post(url"$backendUrl/$zRef/$taxYear/$month")
      .withBody(Json.obj("nilReturn" -> nilReturn))
      .execute[CreateMonthlyReturnResponse]
      .map(response => MonthlyReturn(response.submissionId, nilReturn))

  def updateNilReturn(nilReturn: Boolean, zRef: String, taxYear: String, month: Int)(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturn] =
    httpClient
      .put(url"$backendUrl/$zRef/$taxYear/$month/nilReturn")
      .withBody(Json.obj("value" -> nilReturn))
      .execute[MonthlyReturn]

  def declareMonthlyReturn(zRef: String, taxYear: String, month: Int)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    httpClient
      .post(url"$backendUrl/$zRef/$taxYear/$month/declarations")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT => Future.successful(())
          case status     =>
            Future.failed(UpstreamErrorResponse("declareMonthlyReturn failed", status, status, response.headers))
        }
      }

  def createFileUpload(zRef: String, taxYear: String, month: Int, reference: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    httpClient
      .post(url"$backendUrl/$zRef/$taxYear/$month/files")
      .withBody(Json.obj("reference" -> reference))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case CREATED => Future.successful(())
          case status  =>
            Future.failed(UpstreamErrorResponse("createFileUpload failed", status, status, response.headers))
        }
      }

  def deleteFileUpload(zRef: String, taxYear: String, month: Int, reference: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    httpClient
      .delete(url"$backendUrl/$zRef/$taxYear/$month/files/$reference")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT => Future.successful(())
          case status     =>
            Future.failed(UpstreamErrorResponse("deleteFileUpload failed", status, status, response.headers))
        }
      }
}
