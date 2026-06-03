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

import base.SpecBase

import java.time.{Clock, Instant, ZoneOffset}

class DateHelperSpec extends SpecBase {

  "DateHelper" - {

    "must return the reporting window month from the current date" in {
      val helper = new DateHelper(testReportingWindowClock)

      helper.reportingWindowMonth mustEqual testReportingWindowMonthName
    }

    "must return the reporting period month as the previous month" in {
      val helper = new DateHelper(testReportingWindowClock)

      helper.reportingPeriodMonth mustEqual testReportingPeriodMonthName
    }

    "must return December as the reporting period month for a January reporting window" in {
      val helper = dateHelperAt(testJanuaryReportingWindowInstant)

      helper.reportingPeriodMonth mustEqual previousYearReportingPeriodMonthName
    }

    "must return the submission period enum for the reporting window month" in {
      val helper = new DateHelper(testReportingWindowClock)

      helper.submissionPeriod mustEqual testSubmissionPeriod
    }

    "must return the tax year for a reporting window before April" in {
      val helper = new DateHelper(testReportingWindowClock)

      helper.taxYear mustEqual testTaxYear
    }

    "must return the tax year for a reporting window from April onwards" in {
      val helper = dateHelperAt(testAprilReportingWindowInstant)

      helper.taxYear mustEqual nextTestTaxYear
    }
  }

  private def dateHelperAt(instant: Instant): DateHelper =
    new DateHelper(Clock.fixed(instant, ZoneOffset.UTC))
}
