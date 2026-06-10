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
import models.YesNoAnswer.{No, Yes}
import models.MonthlyReturn
import pages._
import play.api.mvc.Call

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  private def monthlyReturn(nilReturn: Boolean): MonthlyReturn =
    MonthlyReturn(testSubmissionId, nilReturn)

  "Navigator" - {

    "in Normal mode" - {

      "must go from MonthlyReportSubmissionPage to the file upload placeholder when the user is uploading a report" in {
        navigator.nextPage(
          MonthlyReportSubmissionPage,
          NormalMode,
          monthlyReturn(nilReturn = false)
        ) mustBe routes.UploadFileController.onPageLoad()
      }

      "must go from MonthlyReportSubmissionPage to Check Your Answers when the user has a nil report" in {
        navigator.nextPage(
          MonthlyReportSubmissionPage,
          monthlyReturn(nilReturn = true)
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from CheckYourAnswersPage to the declaration placeholder" in {
        navigator.nextPage(
          CheckYourAnswersPage,
          monthlyReturn(nilReturn = true)
        ) mustBe routes.IndexController.onPageLoad()
      }

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, monthlyReturn(nilReturn = false)) mustBe routes.IndexController.onPageLoad()
      }

      "must go from UploadedReportFilesPage to the file upload placeholder when the user wants to add another file" in {
        navigator.nextPage(
          UploadedReportFilesPage,
          Yes
        ) mustBe routes.UploadFileController.onPageLoad()
      }

      "must go from UploadedReportFilesPage to Check Your Answers when the user does not want to add another file" in {
        navigator.nextPage(
          UploadedReportFilesPage,
          No
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}
