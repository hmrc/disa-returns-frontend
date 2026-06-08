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

import connectors.BackendConnector
import models.MonthlyReturn
import play.api.http.Status.{CONFLICT, NOT_FOUND}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.DateHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StorageService @Inject() (
  backendConnector: BackendConnector,
  dateHelper: DateHelper
)(implicit ec: ExecutionContext) {

  def retrieveForThisWindow(zReference: String)(implicit hc: HeaderCarrier): Future[Option[MonthlyReturn]] =
    backendConnector.retrieve(zReference, dateHelper.taxYear, dateHelper.month)

  def saveForThisWindow(
    zReference: String,
    currentMonthlyReturn: Option[MonthlyReturn],
    nilReturn: Boolean
  )(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturn] = {
    val taxYear = dateHelper.taxYear
    val month   = dateHelper.month

    currentMonthlyReturn match {
      case Some(_) => updateWithCreateFallback(zReference, taxYear, month, nilReturn)
      case None    => createWithUpdateFallback(zReference, taxYear, month, nilReturn)
    }
  }

  private def createWithUpdateFallback(zReference: String, taxYear: String, month: Int, nilReturn: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturn] =
    backendConnector
      .createMonthlyReturn(nilReturn, zReference, taxYear, month)
      .recoverWith { case UpstreamErrorResponse(_, CONFLICT, _, _) =>
        backendConnector.updateNilReturn(nilReturn, zReference, taxYear, month)
      }

  private def updateWithCreateFallback(zReference: String, taxYear: String, month: Int, nilReturn: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturn] =
    backendConnector
      .updateNilReturn(nilReturn, zReference, taxYear, month)
      .recoverWith { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
        backendConnector.createMonthlyReturn(nilReturn, zReference, taxYear, month)
      }
}
