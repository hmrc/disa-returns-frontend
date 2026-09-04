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

package testOnly

import base.SpecBase
import _root_.connectors.BackendConnector
import models.ReportingContext
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import testOnly.connectors.TestOnlyReportingOverridesConnector
import testOnly.connectors.TestOnlyReportingOverridesConnector.CurrentOverrides
import services.SystemClock

import java.time.LocalDate
import scala.concurrent.Future

class TestOnlyBackendReportingContextSourceSpec extends SpecBase {
  "TestOnlyBackendReportingContextSource" - {
    "must combine the clock override with normal backend reporting-window status" in {
      val connector        = mock[TestOnlyReportingOverridesConnector]
      val backendConnector = mock[BackendConnector]
      val overrideDate     = LocalDate.parse("2026-06-16")
      when(connector.get(eqTo(testZReference))(any())).thenReturn(
        Future.successful(
          CurrentOverrides(
            systemDate = Some(overrideDate),
            reportingWindow = None
          )
        )
      )
      when(backendConnector.isReportingWindowOpen(eqTo(testZReference))(any())).thenReturn(Future.successful(false))
      val source           = new TestOnlyBackendReportingContextSource(
        connector,
        new SystemClock(testReportingWindowClock),
        backendConnector
      )

      source.get(testZReference).futureValue mustBe ReportingContext(overrideDate, reportingWindowOpen = false)

      verify(connector, times(1)).get(eqTo(testZReference))(any())
      verify(backendConnector, times(1)).isReportingWindowOpen(eqTo(testZReference))(any())
    }

    "must fall back to the frontend system clock when there is no clock override" in {
      val connector        = mock[TestOnlyReportingOverridesConnector]
      val backendConnector = mock[BackendConnector]
      when(connector.get(eqTo(testZReference))(any()))
        .thenReturn(Future.successful(CurrentOverrides(systemDate = None, reportingWindow = None)))
      when(backendConnector.isReportingWindowOpen(eqTo(testZReference))(any())).thenReturn(Future.successful(true))
      val source           = new TestOnlyBackendReportingContextSource(
        connector,
        new SystemClock(testReportingWindowClock),
        backendConnector
      )

      source.get(testZReference).futureValue mustBe ReportingContext(
        LocalDate.now(testReportingWindowClock),
        reportingWindowOpen = true
      )

      verify(connector, times(1)).get(eqTo(testZReference))(any())
      verify(backendConnector, times(1)).isReportingWindowOpen(eqTo(testZReference))(any())
    }
  }
}
