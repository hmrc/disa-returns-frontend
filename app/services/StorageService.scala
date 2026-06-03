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
import models.MonthlyReturnSubmission
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper

import javax.inject.Inject
import scala.concurrent.Future

class StorageService @Inject() (
  backendConnector: BackendConnector,
  dateHelper: DateHelper
) {

  def retrieveForThisWindow(zReference: String)(implicit hc: HeaderCarrier): Future[Option[MonthlyReturnSubmission]] =
    backendConnector.retrieve(zReference, dateHelper.taxYear, dateHelper.submissionPeriod)

  def upsertForThisWindow(zReference: String, monthlyReturnSubmission: MonthlyReturnSubmission)(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturnSubmission] =
    backendConnector.upsert(monthlyReturnSubmission, zReference, dateHelper.taxYear, dateHelper.submissionPeriod)
}
