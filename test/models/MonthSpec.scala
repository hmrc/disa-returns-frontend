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

package models

import models.Month.MAR
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

import java.time.LocalDate

class MonthSpec extends AnyFreeSpec with Matchers {

  "Month" - {

    "must derive the uppercase reporting month from a LocalDate" in {
      Month.fromLocalDate(LocalDate.of(2026, 3, 15)) mustEqual MAR
    }

    "must serialise to the backend enum string" in {
      Json.toJson(MAR) mustEqual JsString("MAR")
    }

    "must deserialise from the backend enum string" in {
      JsString("MAR").validate[Month.Month] mustEqual JsSuccess(MAR)
    }

    "must reject unknown month strings" in {
      JsString("MARCH").validate[Month.Month] mustBe a[JsError]
    }
  }
}
