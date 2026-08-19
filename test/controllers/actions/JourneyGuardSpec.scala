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

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.JourneyGuard.Page
import controllers.actions.JourneyGuard.Page.*
import controllers.routes
import models.requests.OptionalDataRequest
import models.{FileUpload, FileUploadDetails, FileUploadStatus, MonthlyReturn}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class JourneyGuardSpec extends SpecBase {

  private val successfulUpload = FileUpload(
    reference = "successful-reference",
    status = FileUploadStatus.ValidationSuccess,
    fileUploadDetails = Some(FileUploadDetails("return.csv"))
  )

  private val nilReturn          = emptyMonthlyReturn.copy(nilReturn = true)
  private val nonNilWithoutFiles = emptyMonthlyReturn
  private val nonNilWithFiles    = emptyMonthlyReturn.copy(fileUploads = Seq(successfulUpload))
  private val declaredReturn     = nilReturn.copy(declaredOn = Some(Instant.parse("2026-03-15T12:03:00Z")))

  private val requiredPages = Seq(
    UploadFile,
    FileValidationErrors,
    UploadedReportFiles,
    RemoveFile,
    CheckYourAnswers,
    Declaration,
    SubmissionComplete
  )

  private val states = Seq[Option[MonthlyReturn]](
    None,
    Some(nilReturn),
    Some(nonNilWithoutFiles),
    Some(nonNilWithFiles),
    Some(declaredReturn)
  )

  private val allowedStatesByPage = Map[Page, Set[Option[MonthlyReturn]]](
    UploadFile           -> Set(Some(nonNilWithoutFiles), Some(nonNilWithFiles)),
    FileValidationErrors -> Set(Some(nonNilWithoutFiles), Some(nonNilWithFiles)),
    UploadedReportFiles  -> Set(Some(nonNilWithFiles)),
    RemoveFile           -> Set(Some(nonNilWithFiles)),
    CheckYourAnswers     -> Set(Some(nilReturn), Some(nonNilWithFiles)),
    Declaration          -> Set(Some(nilReturn), Some(nonNilWithFiles)),
    SubmissionComplete   -> Set(Some(declaredReturn))
  )

  "JourneyGuard" - {

    "must allow or redirect every guarded page for every open-window journey state" in {
      val application = applicationBuilder().build()

      running(application) {
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val guard     = application.injector.instanceOf[JourneyGuard]

        requiredPages.foreach { page =>
          states.foreach { monthlyReturn =>
            val request = OptionalDataRequest(FakeRequest(), testZReference, testUserDetails, monthlyReturn)
            val result  = guard(page).invokeBlock(request, _ => Future.successful(Ok(""))).futureValue

            if (allowedStatesByPage(page).contains(monthlyReturn)) {
              result.header.status mustEqual OK
            } else {
              result.header.status mustEqual SEE_OTHER
              result.header.headers(LOCATION) mustEqual expectedCanonicalUrl(monthlyReturn, appConfig)
            }
          }
        }
      }
    }

    "must allow the initial question for every undeclared open-window state" in {
      val application = applicationBuilder().build()

      running(application) {
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val guard     = application.injector.instanceOf[JourneyGuard]

        states.foreach { monthlyReturn =>
          val request = OptionalDataRequest(FakeRequest(), testZReference, testUserDetails, monthlyReturn)
          val result  = guard
            .optionalData(MonthlyReportSubmission)
            .invokeBlock(request, _ => Future.successful(Ok("")))
            .futureValue

          if (monthlyReturn.contains(declaredReturn)) {
            result.header.status mustEqual SEE_OTHER
            result.header.headers(LOCATION) mustEqual appConfig.manageIsasUrl
          } else {
            result.header.status mustEqual OK
          }
        }
      }
    }

    "must allow the index default only when no monthly return exists" in {
      val application = applicationBuilder().build()

      running(application) {
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val guard     = application.injector.instanceOf[JourneyGuard]

        states.foreach { monthlyReturn =>
          val request = OptionalDataRequest(FakeRequest(), testZReference, testUserDetails, monthlyReturn)
          val result  = guard
            .optionalData(Index)
            .invokeBlock(request, _ => Future.successful(Ok("")))
            .futureValue

          if (monthlyReturn.isEmpty) {
            result.header.status mustEqual OK
          } else {
            result.header.status mustEqual SEE_OTHER
            result.header.headers(LOCATION) mustEqual expectedCanonicalUrl(monthlyReturn, appConfig)
          }
        }
      }
    }

    "must redirect every guarded page to Manage ISAs when the reporting window is closed" in {
      val closedClock = Clock.fixed(Instant.parse("2026-03-05T12:00:00Z"), ZoneOffset.UTC)
      val application = applicationBuilder().build()

      running(application) {
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val guard     = new JourneyGuard(appConfig, closedClock)
        val request   = OptionalDataRequest(FakeRequest(), testZReference, testUserDetails, Some(nonNilWithFiles))

        requiredPages.foreach { page =>
          val result = guard(page).invokeBlock(request, _ => Future.successful(Ok(""))).futureValue

          result.header.status mustEqual SEE_OTHER
          result.header.headers(LOCATION) mustEqual appConfig.manageIsasUrl
        }

        Seq(MonthlyReportSubmission, Index).foreach { page =>
          val optionalResult = guard
            .optionalData(page)
            .invokeBlock(request, _ => Future.successful(Ok("")))
            .futureValue

          optionalResult.header.status mustEqual SEE_OTHER
          optionalResult.header.headers(LOCATION) mustEqual appConfig.manageIsasUrl
        }
      }
    }
  }

  private def expectedCanonicalUrl(monthlyReturn: Option[MonthlyReturn], appConfig: FrontendAppConfig): String =
    monthlyReturn match {
      case Some(value) if value.isDeclared                                        =>
        appConfig.manageIsasUrl
      case Some(value) if value.nilReturn || value.successfulFileUploads.nonEmpty =>
        routes.CheckYourAnswersController.onPageLoad().url
      case _                                                                      =>
        appConfig.manageIsasUrl
    }
}
