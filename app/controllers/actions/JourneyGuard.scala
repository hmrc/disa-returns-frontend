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

package controllers.actions

import config.FrontendAppConfig
import controllers.routes
import models.MonthlyReturn
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, ActionRefiner, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object JourneyGuard {

  sealed trait Page

  object Page {
    case object Index extends Page
    case object MonthlyReportSubmission extends Page
    case object UploadFile extends Page
    case object FileValidationErrors extends Page
    case object UploadedReportFiles extends Page
    case object RemoveFile extends Page
    case object CheckYourAnswers extends Page
    case object Declaration extends Page
    case object SubmissionComplete extends Page
  }

  private sealed trait State

  private object State {
    case object ReportingWindowClosed extends State
    case object NoMonthlyReturn extends State
    case object UndeclaredNilReturn extends State
    case object UndeclaredNonNilWithoutFiles extends State
    case object UndeclaredNonNilWithValidFiles extends State
    case object DeclaredReturn extends State
  }
}

@Singleton
class JourneyGuard @Inject() (
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) {

  import JourneyGuard.Page
  import JourneyGuard.Page.*
  import JourneyGuard.State
  import JourneyGuard.State.*

  def optionalData(page: Page): ActionFilter[OptionalDataRequest] =
    new ActionFilter[OptionalDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] =
        Future.successful(
          redirectFor(page, request.monthlyReturn, request.reportingWindowOpen).map(Redirect(_))
        )
    }

  def apply(page: Page): ActionRefiner[OptionalDataRequest, DataRequest] =
    new ActionRefiner[OptionalDataRequest, DataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] =
        Future.successful(
          redirectFor(page, request.monthlyReturn, request.reportingWindowOpen) match {
            case Some(destination) =>
              Left(Redirect(destination))
            case None              =>
              request.monthlyReturn match {
                case Some(monthlyReturn) =>
                  Right(
                    DataRequest(
                      request.request,
                      request.zReference,
                      request.userDetails,
                      monthlyReturn,
                      request.currentDate,
                      request.reportingWindowOpen
                    )
                  )
                case None                =>
                  Left(Redirect(appConfig.manageIsasUrl))
              }
          }
        )
    }

  private def redirectFor(
    page: Page,
    monthlyReturn: Option[MonthlyReturn],
    reportingWindowOpen: Boolean
  ): Option[String] = {
    val currentState = state(monthlyReturn, reportingWindowOpen)

    Option.unless(isAllowed(page, currentState))(recoveryUrl(currentState))
  }

  private def state(monthlyReturn: Option[MonthlyReturn], reportingWindowOpen: Boolean): State =
    if (!reportingWindowOpen) {
      ReportingWindowClosed
    } else {
      monthlyReturn match {
        case None                                                => NoMonthlyReturn
        case Some(value) if value.isDeclared                     => DeclaredReturn
        case Some(value) if value.nilReturn                      => UndeclaredNilReturn
        case Some(value) if value.successfulFileUploads.nonEmpty => UndeclaredNonNilWithValidFiles
        case Some(_)                                             => UndeclaredNonNilWithoutFiles
      }
    }

  private def recoveryUrl(state: State): String =
    state match {
      case ReportingWindowClosed | NoMonthlyReturn | UndeclaredNonNilWithoutFiles | DeclaredReturn =>
        appConfig.manageIsasUrl
      case UndeclaredNilReturn | UndeclaredNonNilWithValidFiles                                    =>
        routes.CheckYourAnswersController.onPageLoad().url
    }

  private def isAllowed(page: Page, state: State): Boolean =
    page match {
      case Index                             =>
        state == NoMonthlyReturn
      case MonthlyReportSubmission           =>
        Set(NoMonthlyReturn, UndeclaredNilReturn, UndeclaredNonNilWithoutFiles, UndeclaredNonNilWithValidFiles)
          .contains(state)
      case UploadFile | FileValidationErrors =>
        Set(UndeclaredNonNilWithoutFiles, UndeclaredNonNilWithValidFiles).contains(state)
      case UploadedReportFiles | RemoveFile  =>
        state == UndeclaredNonNilWithValidFiles
      case CheckYourAnswers | Declaration    =>
        Set(UndeclaredNilReturn, UndeclaredNonNilWithValidFiles).contains(state)
      case SubmissionComplete                =>
        state == DeclaredReturn
    }

}
