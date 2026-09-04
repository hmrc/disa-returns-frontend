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
import models.ReportingContext
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future

class ReportingContextSourceSpec extends SpecBase {
  "SystemReportingContextSource" - {
    "must combine the local system date and backend reporting-window status" in {
      val backendConnector = mock[BackendConnector]
      when(backendConnector.isReportingWindowOpen(eqTo(testZReference))(any()))
        .thenReturn(Future.successful(false))
      val source           = new SystemReportingContextSource(
        new SystemClock(testReportingWindowClock),
        backendConnector
      )

      source.get(testZReference).futureValue mustBe ReportingContext(
        testReportingWindowInstant.atZone(testReportingWindowClock.getZone).toLocalDate,
        reportingWindowOpen = false
      )
      verify(backendConnector, times(1)).isReportingWindowOpen(eqTo(testZReference))(any())
    }
  }
}
