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

package controllers

import base.SpecBase
import models.{FileUpload, FileUploadStatus, MonthlyReturn}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.Instant

class IndexControllerSpec extends SpecBase {

  private val nilReturn          = emptyMonthlyReturn.copy(nilReturn = true)
  private val nonNilWithoutFiles = emptyMonthlyReturn
  private val nonNilWithFiles    = emptyMonthlyReturn.copy(
    fileUploads = Seq(FileUpload("successful-reference", FileUploadStatus.ValidationSuccess))
  )
  private val declaredReturn     = nilReturn.copy(declaredOn = Some(Instant.parse("2026-03-15T12:03:00Z")))

  "IndexController" - {

    "must default a new journey to the monthly report question" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, routes.IndexController.onPageLoad().url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.MonthlyReportSubmissionController.onPageLoad().url
      }
    }

    "must use the journey guard for an existing journey" in {
      val cases = Seq[(MonthlyReturn, Option[String])](
        nilReturn          -> Some(routes.CheckYourAnswersController.onPageLoad().url),
        nonNilWithoutFiles -> None,
        nonNilWithFiles    -> Some(routes.CheckYourAnswersController.onPageLoad().url),
        declaredReturn     -> None
      )

      cases.foreach { case (monthlyReturn, expectedDestination) =>
        val application = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

        running(application) {
          val result   = route(application, FakeRequest(GET, routes.IndexController.onPageLoad().url)).value
          val expected = expectedDestination.getOrElse(manageIsasUrl(application))

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual expected
        }
      }
    }
  }
}
