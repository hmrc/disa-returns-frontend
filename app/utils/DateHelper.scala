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

package utils

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.{Clock, LocalDate}
import java.util.Locale
import javax.inject.{Inject, Singleton}

@Singleton
class DateHelper @Inject() (
  clock: Clock
) {

  private def reportingWindowDate: LocalDate = LocalDate.now(clock)

  private def reportingPeriodDate: LocalDate = reportingWindowDate.minusMonths(1)

  def reportingPeriod: String =
    reportingPeriodDate.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.UK))

  private def monthName(date: LocalDate): String =
    date.getMonth.getDisplayName(TextStyle.FULL, Locale.UK)

  def reportingWindowMonth: String =
    monthName(reportingWindowDate)

  def reportingPeriodMonth: String =
    monthName(reportingPeriodDate)

  def reportingWindowMonthNumber: Int = reportingWindowDate.getMonthValue

  def reportingWindowTaxYear: String = {
    val startYear =
      if (reportingWindowDate.getMonthValue >= 4) reportingWindowDate.getYear else reportingWindowDate.getYear - 1
    s"$startYear-${(startYear + 1).toString.takeRight(2)}"
  }
}
