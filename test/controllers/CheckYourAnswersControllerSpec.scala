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

package controllers

import base.SpecBase
import models.{FileUpload, FileUploadDetails, FileUploadStatus, MonthlyReturn}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._

class CheckYourAnswersControllerSpec extends SpecBase {

  private val onwardRoute = Call("GET", "/foo")

  private val successfulUploadOne = FileUpload(
    reference = "successful-reference-1",
    status = FileUploadStatus.UpscanSuccess,
    fileUploadDetails = Some(FileUploadDetails("file1.csv"))
  )

  private val successfulUploadTwo = FileUpload(
    reference = "successful-reference-2",
    status = FileUploadStatus.UpscanSuccess,
    fileUploadDetails = Some(FileUploadDetails("file2.csv"))
  )

  private val inProgressUpload = FileUpload(
    reference = "in-progress-reference",
    status = "CREATED"
  )

  private def applicationWith(monthlyReturn: Option[MonthlyReturn]) =
    applicationBuilder(monthlyReturn = monthlyReturn)
      .overrides(
        bind[java.time.Clock].toInstance(testReportingWindowClock),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

  "Check Your Answers Controller" - {

    "must return OK and show the nil return answer without a files row" in {

      val monthlyReturn = emptyMonthlyReturn.copy(nilReturn = true)
      val application   = applicationWith(Some(monthlyReturn))

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result  = route(application, request).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include("Check your answers")
        content must include("Are you submitting a report file for March window?")
        content must include("No - I have a nil report")
        content must include(routes.MonthlyReportSubmissionController.onPageLoad().url)
        content must not include "Files"
        content must not include routes.UploadedReportFilesController.onPageLoad().url
      }
    }

    "must return OK and show the report answer and all successful uploaded file names" in {

      val monthlyReturn = emptyMonthlyReturn.copy(
        nilReturn = false,
        fileUploads = Seq(successfulUploadOne, inProgressUpload, successfulUploadTwo)
      )
      val application   = applicationWith(Some(monthlyReturn))

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result  = route(application, request).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include("Check your answers")
        content must include("Are you submitting a report file for March window?")
        content must include("Yes - I am uploading a report")
        content must include("Files")
        content must include("file1.csv")
        content must include("file2.csv")
        content must not include "in-progress-reference"
        content must include(routes.MonthlyReportSubmissionController.onPageLoad().url)
        content must include(routes.UploadedReportFilesController.onPageLoad().url)
      }
    }

    "must redirect to the next page when Save and continue is selected" in {

      val application = applicationWith(Some(emptyMonthlyReturn.copy(nilReturn = true)))

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationWith(None)

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationWith(None)

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
