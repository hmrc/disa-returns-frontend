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

package testOnly.controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import testOnly.connectors.TestOnlyReportingOverridesConnector
import testOnly.connectors.TestOnlyReportingOverridesConnector.CurrentOverrides
import testOnly.forms.TestOnlyReportingOverrides

import java.time.LocalDate
import scala.concurrent.Future

class TestOnlyReportingOverridesControllerSpec extends SpecBase {
  "TestOnlyReportingOverridesController" - {
    "must reload the active backend overrides into the form" in {
      val connector   = mock[TestOnlyReportingOverridesConnector]
      val systemDate  = LocalDate.parse("2026-06-17")
      when(connector.get(eqTo(testZReference))(any())).thenReturn(
        Future.successful(
          CurrentOverrides(
            systemDate = Some(systemDate),
            reportingWindow = Some(LocalDate.parse("2026-06-06") -> LocalDate.parse("2026-06-19"))
          )
        )
      )
      val application = applicationBuilder()
        .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .overrides(bind[TestOnlyReportingOverridesConnector].toInstance(connector))
        .build()

      running(application) {
        val result = route(
          application,
          FakeRequest(GET, "/obligations/returns/isa/test-only/reporting-overrides")
        ).value

        status(result) mustBe OK
        val html = contentAsString(result)
        inputValue(html, "reportingWindowStart.day") mustBe "6"
        inputValue(html, "reportingWindowStart.month") mustBe "6"
        inputValue(html, "reportingWindowStart.year") mustBe "2026"
        inputValue(html, "reportingWindowEnd.day") mustBe "19"
        inputValue(html, "reportingWindowEnd.month") mustBe "6"
        inputValue(html, "reportingWindowEnd.year") mustBe "2026"
        inputValue(html, "systemDate.day") mustBe "17"
        inputValue(html, "systemDate.month") mustBe "6"
        inputValue(html, "systemDate.year") mustBe "2026"
      }
    }

    "must leave the system date empty when there is no active clock override" in {
      val connector   = mock[TestOnlyReportingOverridesConnector]
      when(connector.get(eqTo(testZReference))(any())).thenReturn(
        Future.successful(
          CurrentOverrides(
            systemDate = None,
            reportingWindow = Some(LocalDate.parse("2026-06-06") -> LocalDate.parse("2026-06-19"))
          )
        )
      )
      val application = applicationBuilder()
        .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .overrides(bind[TestOnlyReportingOverridesConnector].toInstance(connector))
        .build()

      running(application) {
        val result = route(
          application,
          FakeRequest(GET, "/obligations/returns/isa/test-only/reporting-overrides")
        ).value

        status(result) mustBe OK
        val html = contentAsString(result)
        inputValue(html, "reportingWindowStart.day") mustBe "6"
        inputValue(html, "reportingWindowEnd.day") mustBe "19"
        inputValue(html, "systemDate.day") mustBe empty
        inputValue(html, "systemDate.month") mustBe empty
        inputValue(html, "systemDate.year") mustBe empty
      }
    }

    "must reset the clock override when submitted with an empty system date" in {
      val connector   = mock[TestOnlyReportingOverridesConnector]
      val overrides   = TestOnlyReportingOverrides(
        LocalDate.parse("2026-06-06"),
        LocalDate.parse("2026-06-19"),
        None
      )
      when(connector.set(eqTo(testZReference), eqTo(overrides))(any())).thenReturn(Future.successful(()))
      val application = applicationBuilder()
        .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .overrides(bind[TestOnlyReportingOverridesConnector].toInstance(connector))
        .build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, "/obligations/returns/isa/test-only/reporting-overrides")
            .withFormUrlEncodedBody(
              "reportingWindowStart.day"   -> "6",
              "reportingWindowStart.month" -> "6",
              "reportingWindowStart.year"  -> "2026",
              "reportingWindowEnd.day"     -> "19",
              "reportingWindowEnd.month"   -> "6",
              "reportingWindowEnd.year"    -> "2026",
              "systemDate.day"             -> "",
              "systemDate.month"           -> "",
              "systemDate.year"            -> ""
            )
        ).value

        status(result) mustBe SEE_OTHER
        verify(connector).set(eqTo(testZReference), eqTo(overrides))(any())
      }
    }
  }

  private def inputValue(html: String, name: String): String = {
    val input = """<input[^>]*>""".r.findAllIn(html).find(_.contains(s"name=\"$name\"")).value
    """value="([^"]*)""".r.findFirstMatchIn(input).map(_.group(1)).getOrElse("")
  }
}
