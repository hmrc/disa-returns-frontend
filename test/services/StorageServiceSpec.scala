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

import base.SpecBase
import connectors.BackendConnector
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper

import scala.concurrent.Future

class StorageServiceSpec extends SpecBase with MockitoSugar {

  private val dateHelper = new DateHelper(testReportingWindowClock)

  "StorageService" - {

    "must retrieve a submission for the current reporting window" in {
      val connector = mock[BackendConnector]
      when(connector.retrieve(eqTo(testZReference), eqTo(testTaxYear), eqTo(testSubmissionPeriod))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emptyMonthlyReturnSubmission)))
      val service   = new StorageService(connector, dateHelper)

      val result = service.retrieveForThisWindow(testZReference)(HeaderCarrier()).futureValue

      result.value mustEqual emptyMonthlyReturnSubmission
      verify(connector).retrieve(eqTo(testZReference), eqTo(testTaxYear), eqTo(testSubmissionPeriod))(
        any[HeaderCarrier]
      )
    }

    "must upsert a submission for the current reporting window" in {
      val connector = mock[BackendConnector]
      when(
        connector.upsert(
          eqTo(emptyMonthlyReturnSubmission),
          eqTo(testZReference),
          eqTo(testTaxYear),
          eqTo(testSubmissionPeriod)
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(emptyMonthlyReturnSubmission))
      val service   = new StorageService(connector, dateHelper)

      val result =
        service.upsertForThisWindow(testZReference, emptyMonthlyReturnSubmission)(HeaderCarrier()).futureValue

      result mustEqual emptyMonthlyReturnSubmission
      verify(connector).upsert(
        eqTo(emptyMonthlyReturnSubmission),
        eqTo(testZReference),
        eqTo(testTaxYear),
        eqTo(testSubmissionPeriod)
      )(any[HeaderCarrier])
    }
  }
}
