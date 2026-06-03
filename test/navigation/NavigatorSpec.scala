/*
 * Copyright 2025 HM Revenue & Customs
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

package navigation

import base.SpecBase
import controllers.routes
import models.{CheckMode, MonthlyReturnSubmission, NormalMode}
import pages._
import play.api.mvc.Call

import java.util.UUID

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  private def submission(nilReport: Boolean): MonthlyReturnSubmission =
    MonthlyReturnSubmission(UUID.fromString("11111111-1111-1111-1111-111111111111"), nilReport)

  "Navigator" - {

    "in Normal mode" - {

      "must go from MonthlyReportSubmissionPage to the file upload placeholder when the user is uploading a report" in {
        navigator.nextPage(MonthlyReportSubmissionPage, NormalMode, submission(nilReport = false)) mustBe Call(
          "GET",
          "???"
        )
      }

      "must go from MonthlyReportSubmissionPage to the nil report CYA placeholder when the user has a nil report" in {
        navigator.nextPage(MonthlyReportSubmissionPage, NormalMode, submission(nilReport = true)) mustBe Call(
          "GET",
          "???"
        )
      }

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, submission(nilReport = false)) mustBe routes.IndexController
          .onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          submission(nilReport = false)
        ) mustBe routes.CheckYourAnswersController
          .onPageLoad()
      }
    }
  }
}
