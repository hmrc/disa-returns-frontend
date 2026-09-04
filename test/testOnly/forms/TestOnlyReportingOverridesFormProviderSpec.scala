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

package testOnly.forms

import base.SpecBase
import play.api.i18n.Messages

import java.time.LocalDate

class TestOnlyReportingOverridesFormProviderSpec extends SpecBase {
  private val app                                 = applicationBuilder().build()
  implicit private val implicitMessages: Messages = messages(app)
  private val form                                = new TestOnlyReportingOverridesFormProvider()()

  "TestOnlyReportingOverridesFormProvider" - {
    "must bind valid overrides" in {
      form.bind(validData).value.value mustBe TestOnlyReportingOverrides(
        LocalDate.parse("2026-06-06"),
        LocalDate.parse("2026-06-19"),
        Some(LocalDate.parse("2026-06-10"))
      )
    }

    "must bind an empty system date as no clock override" in {
      val data = validData ++ Map(
        "systemDate.day"   -> "",
        "systemDate.month" -> "",
        "systemDate.year"  -> ""
      )

      form.bind(data).value.value mustBe TestOnlyReportingOverrides(
        LocalDate.parse("2026-06-06"),
        LocalDate.parse("2026-06-19"),
        None
      )
    }

    "must reject a reporting window whose start is after its end" in {
      val data = validData ++ Map(
        "reportingWindowStart.day" -> "20",
        "reportingWindowEnd.day"   -> "19"
      )

      form.bind(data).errors.map(_.message) must contain("testOnly.reportingOverrides.window.invalid")
    }
  }

  private def validData: Map[String, String] = Map(
    "reportingWindowStart.day"   -> "6",
    "reportingWindowStart.month" -> "6",
    "reportingWindowStart.year"  -> "2026",
    "reportingWindowEnd.day"     -> "19",
    "reportingWindowEnd.month"   -> "6",
    "reportingWindowEnd.year"    -> "2026",
    "systemDate.day"             -> "10",
    "systemDate.month"           -> "6",
    "systemDate.year"            -> "2026"
  )
}
