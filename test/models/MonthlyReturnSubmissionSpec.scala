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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.util.UUID

class MonthlyReturnSubmissionSpec extends AnyFreeSpec with Matchers {

  "MonthlyReturnSubmission" - {

    "must round-trip through JSON" in {
      val submission = MonthlyReturnSubmission(
        submissionId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        nilReport = true
      )

      Json.toJson(submission).validate[MonthlyReturnSubmission] mustEqual JsSuccess(submission)
    }
  }
}
