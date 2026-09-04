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

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

final case class TestOnlyReportingOverrides(
  reportingWindowStart: LocalDate,
  reportingWindowEnd: LocalDate,
  systemDate: Option[LocalDate]
)

class TestOnlyReportingOverridesFormProvider @Inject() extends Mappings {
  def apply()(implicit messages: Messages): Form[TestOnlyReportingOverrides] =
    Form(
      mapping(
        "reportingWindowStart" -> date("reportingWindowStart"),
        "reportingWindowEnd"   -> date("reportingWindowEnd"),
        "systemDate"           -> optional(date("systemDate"))
      )(TestOnlyReportingOverrides.apply)(value => Some(Tuple.fromProductTyped(value)))
        .verifying(
          "testOnly.reportingOverrides.window.invalid",
          value => !value.reportingWindowStart.isAfter(value.reportingWindowEnd)
        )
    )

  private def date(field: String)(implicit messages: Messages) =
    localDate(
      invalidKey = s"testOnly.reportingOverrides.$field.invalid",
      allRequiredKey = s"testOnly.reportingOverrides.$field.required",
      twoRequiredKey = s"testOnly.reportingOverrides.$field.required",
      requiredKey = s"testOnly.reportingOverrides.$field.required"
    )
}
