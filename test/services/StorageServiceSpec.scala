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
import play.api.http.Status.{CONFLICT, NOT_FOUND}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.DateHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorageServiceSpec extends SpecBase with MockitoSugar {

  private val dateHelper = new DateHelper(testReportingWindowClock)

  "StorageService" - {

    "must retrieve a monthly return for the current reporting window" in {
      val connector = mock[BackendConnector]
      when(connector.retrieve(eqTo(testZReference), eqTo(testTaxYear), eqTo(testMonth))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emptyMonthlyReturn)))
      val service   = new StorageService(connector, dateHelper)

      val result = service.retrieveForThisWindow(testZReference)(HeaderCarrier()).futureValue

      result.value mustEqual emptyMonthlyReturn
      verify(connector).retrieve(eqTo(testZReference), eqTo(testTaxYear), eqTo(testMonth))(
        any[HeaderCarrier]
      )
    }

    "must create a monthly return when there is no existing monthly return for the current reporting window" in {
      val connector = mock[BackendConnector]
      when(
        connector.createMonthlyReturn(
          eqTo(true),
          eqTo(testZReference),
          eqTo(testTaxYear),
          eqTo(testMonth)
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(emptyMonthlyReturn))
      val service   = new StorageService(connector, dateHelper)

      val result =
        service.saveForThisWindow(testZReference, None, nilReturn = true)(HeaderCarrier()).futureValue

      result mustEqual emptyMonthlyReturn
      verify(connector).createMonthlyReturn(
        eqTo(true),
        eqTo(testZReference),
        eqTo(testTaxYear),
        eqTo(testMonth)
      )(any[HeaderCarrier])
    }

    "must update nilReturn when a monthly return already exists for the current reporting window" in {
      val connector = mock[BackendConnector]
      when(
        connector.updateNilReturn(
          eqTo(false),
          eqTo(testZReference),
          eqTo(testTaxYear),
          eqTo(testMonth)
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(emptyMonthlyReturn))
      val service   = new StorageService(connector, dateHelper)

      val result =
        service
          .saveForThisWindow(testZReference, Some(emptyMonthlyReturn), nilReturn = false)(HeaderCarrier())
          .futureValue

      result mustEqual emptyMonthlyReturn
      verify(connector).updateNilReturn(
        eqTo(false),
        eqTo(testZReference),
        eqTo(testTaxYear),
        eqTo(testMonth)
      )(any[HeaderCarrier])
    }

    "must update nilReturn when create conflicts because the monthly return was created after retrieval" in {
      val connector = mock[BackendConnector]
      when(
        connector.createMonthlyReturn(
          eqTo(true),
          eqTo(testZReference),
          eqTo(testTaxYear),
          eqTo(testMonth)
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.failed(UpstreamErrorResponse("conflict", CONFLICT, CONFLICT)))
      when(
        connector.updateNilReturn(
          eqTo(true),
          eqTo(testZReference),
          eqTo(testTaxYear),
          eqTo(testMonth)
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(emptyMonthlyReturn))
      val service   = new StorageService(connector, dateHelper)

      val result =
        service.saveForThisWindow(testZReference, None, nilReturn = true)(HeaderCarrier()).futureValue

      result mustEqual emptyMonthlyReturn
      verify(connector).createMonthlyReturn(
        eqTo(true),
        eqTo(testZReference),
        eqTo(testTaxYear),
        eqTo(testMonth)
      )(any[HeaderCarrier])
      verify(connector).updateNilReturn(
        eqTo(true),
        eqTo(testZReference),
        eqTo(testTaxYear),
        eqTo(testMonth)
      )(any[HeaderCarrier])
    }

    "must create the monthly return when update fails because the return was missing after retrieval" in {
      val connector = mock[BackendConnector]
      when(
        connector.updateNilReturn(
          eqTo(false),
          eqTo(testZReference),
          eqTo(testTaxYear),
          eqTo(testMonth)
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND, NOT_FOUND)))
      when(
        connector.createMonthlyReturn(
          eqTo(false),
          eqTo(testZReference),
          eqTo(testTaxYear),
          eqTo(testMonth)
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(emptyMonthlyReturn))
      val service   = new StorageService(connector, dateHelper)

      val result =
        service
          .saveForThisWindow(testZReference, Some(emptyMonthlyReturn), nilReturn = false)(HeaderCarrier())
          .futureValue

      result mustEqual emptyMonthlyReturn
      verify(connector).updateNilReturn(
        eqTo(false),
        eqTo(testZReference),
        eqTo(testTaxYear),
        eqTo(testMonth)
      )(any[HeaderCarrier])
      verify(connector).createMonthlyReturn(
        eqTo(false),
        eqTo(testZReference),
        eqTo(testTaxYear),
        eqTo(testMonth)
      )(any[HeaderCarrier])
    }
  }
}
