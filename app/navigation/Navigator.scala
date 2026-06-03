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

import javax.inject.{Inject, Singleton}

import play.api.mvc.Call
import controllers.routes
import pages._
import models._

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => MonthlyReturnSubmission => Call = {
    case MonthlyReportSubmissionPage =>
      answers => monthlyReportSubmissionRoute(answers)
    case _                           => _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => MonthlyReturnSubmission => Call = _ =>
    _ => routes.CheckYourAnswersController.onPageLoad()

  private def monthlyReportSubmissionRoute(monthlyReturnSubmission: MonthlyReturnSubmission): Call =
    if (monthlyReturnSubmission.nilReport) {
      nilReportCheckYourAnswersRoute
    } else {
      fileUploadJourneyRoute
    }

  private def fileUploadJourneyRoute: Call =
    // TODO DFI-2156: replace placeholder with file upload journey route.
    Call("GET", "???")

  private def nilReportCheckYourAnswersRoute: Call =
    // TODO DFI-2120: replace placeholder with nil report CYA journey route.
    Call("GET", "???")

  def nextPage(page: Page, mode: Mode, monthlyReturnSubmission: MonthlyReturnSubmission): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(monthlyReturnSubmission)
    case CheckMode  =>
      checkRouteMap(page)(monthlyReturnSubmission)
  }
}
