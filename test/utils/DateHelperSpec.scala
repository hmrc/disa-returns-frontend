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

import models.Month
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.{Clock, Instant, ZoneOffset}

class DateHelperSpec extends AnyFreeSpec with Matchers {

  "DateHelper" - {

    "must return the reporting window month from the current date" in {
      val helper = dateHelperAt("2026-03-15T12:00:00Z")

      helper.reportingWindowMonth mustEqual "March"
    }

    "must return the reporting period month as the previous month" in {
      val helper = dateHelperAt("2026-03-15T12:00:00Z")

      helper.reportingPeriodMonth mustEqual "February"
    }

    "must return December as the reporting period month for a January reporting window" in {
      val helper = dateHelperAt("2026-01-15T12:00:00Z")

      helper.reportingPeriodMonth mustEqual "December"
    }

    "must return the submission period enum for the reporting window month" in {
      val helper = dateHelperAt("2026-03-15T12:00:00Z")

      helper.submissionPeriod mustEqual Month.MAR
    }

    "must return the tax year for a reporting window before April" in {
      val helper = dateHelperAt("2026-03-15T12:00:00Z")

      helper.taxYear mustEqual "2025-26"
    }

    "must return the tax year for a reporting window from April onwards" in {
      val helper = dateHelperAt("2026-04-01T00:00:00Z")

      helper.taxYear mustEqual "2026-27"
    }
  }

  private def dateHelperAt(instant: String): DateHelper =
    new DateHelper(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC))
}
