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

import controllers.routes
import models._
import pages._
import play.api.mvc.Call

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => MonthlyReturn => Call = {
    case MonthlyReportSubmissionPage =>
      monthlyReturn => monthlyReturnRoute(monthlyReturn)
    case CheckYourAnswersPage        => monthlyReturn => declarationRoute(monthlyReturn)
    case _                           => _ => routes.IndexController.onPageLoad()
  }

  private def monthlyReturnRoute(monthlyReturn: MonthlyReturn): Call =
    if (monthlyReturn.nilReturn) {
      checkYourAnswersRoute
    } else {
      fileUploadJourneyRoute
    }

  private def fileUploadJourneyRoute: Call =
    routes.UploadFileController.onPageLoad()

  private def uploadedReportFilesRoute(answer: YesNoAnswer): Call =
    answer match {
      case YesNoAnswer.Yes => fileUploadJourneyRoute
      case YesNoAnswer.No  => checkYourAnswersRoute
    }

  private def checkYourAnswersRoute: Call =
    routes.CheckYourAnswersController.onPageLoad()

  private def declarationRoute(monthlyReturn: MonthlyReturn): Call =
    routes.DeclarationController.onPageLoad(monthlyReturn.nilReturn)

  def nextPage(page: Page, monthlyReturn: MonthlyReturn): Call =
    normalRoutes(page)(monthlyReturn)

  def nextPage(page: Page, answer: YesNoAnswer): Call =
    page match {
      case UploadedReportFilesPage => uploadedReportFilesRoute(answer)
      case _                       => routes.IndexController.onPageLoad()
    }
}
