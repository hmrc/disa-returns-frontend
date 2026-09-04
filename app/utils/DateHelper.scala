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

  private def reportingPeriodDate(date: LocalDate): LocalDate = date.minusMonths(1)

  def reportingPeriod: String = reportingPeriod(reportingWindowDate)

  def reportingPeriod(date: LocalDate): String =
    reportingPeriodDate(date).format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.UK))

  private def monthName(date: LocalDate): String =
    date.getMonth.getDisplayName(TextStyle.FULL, Locale.UK)

  def reportingWindowMonth: String = reportingWindowMonth(reportingWindowDate)

  def reportingWindowMonth(date: LocalDate): String = monthName(date)

  def reportingPeriodMonth: String = reportingPeriodMonth(reportingWindowDate)

  def reportingPeriodMonth(date: LocalDate): String = monthName(reportingPeriodDate(date))

  def reportingWindowMonthNumber: Int = reportingWindowDate.getMonthValue

  def reportingPeriodMonthNumber: Int = reportingPeriodMonthNumber(reportingWindowDate)

  def reportingPeriodMonthNumber(date: LocalDate): Int = reportingPeriodDate(date).getMonthValue

  def reportingWindowTaxYear: String = taxYearFor(reportingWindowDate)

  def reportingPeriodTaxYear: String = reportingPeriodTaxYear(reportingWindowDate)

  def reportingPeriodTaxYear(date: LocalDate): String = taxYearFor(reportingPeriodDate(date))

  private def taxYearFor(date: LocalDate): String = {
    val startYear =
      if (date.getMonthValue >= 4) date.getYear else date.getYear - 1
    s"$startYear-${(startYear + 1).toString.takeRight(2)}"
  }
}
