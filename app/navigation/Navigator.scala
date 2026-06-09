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

  private val normalRoutes: Page => MonthlyReturn => Call = {
    case MonthlyReportSubmissionPage =>
      monthlyReturn => monthlyReturnRoute(monthlyReturn)
    case _                           => _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => MonthlyReturn => Call = _ => _ => routes.CheckYourAnswersController.onPageLoad()

  private def monthlyReturnRoute(monthlyReturn: MonthlyReturn): Call =
    if (monthlyReturn.nilReturn) {
      nilReturnCheckYourAnswersRoute
    } else {
      fileUploadJourneyRoute
    }

  private def fileUploadJourneyRoute: Call =
    // TODO DFI-2156: replace placeholder with file upload journey route.
    routes.UploadFileController.onPageLoad()

  private def nilReturnCheckYourAnswersRoute: Call =
    // TODO DFI-2120: replace placeholder with nil report CYA journey route.
    routes.IndexController.onPageLoad()

  def nextPage(page: Page, mode: Mode, monthlyReturn: MonthlyReturn): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(monthlyReturn)
    case CheckMode  =>
      checkRouteMap(page)(monthlyReturn)
  }
}
