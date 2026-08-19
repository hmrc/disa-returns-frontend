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
import forms.UploadedReportFilesFormProvider
import models.YesNoAnswer.{No, Yes}
import models.{FileUpload, FileUploadDetails, FileUploadStatus}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.UploadedReportFilesView

class UploadedReportFilesControllerSpec extends SpecBase {

  private val onwardRoute = Call("GET", "/foo")

  private val formProvider = new UploadedReportFilesFormProvider()
  private val form         = formProvider()

  private val successfulUpload = FileUpload(
    reference = "successful-reference",
    status = FileUploadStatus.ValidationSuccess,
    fileUploadDetails = Some(FileUploadDetails("return.csv"))
  )

  private val inProgressUpload = FileUpload(
    reference = "in-progress-reference",
    status = "CREATED"
  )

  private val monthlyReturn =
    emptyMonthlyReturn.copy(fileUploads = Seq(successfulUpload, inProgressUpload))

  private val uploadedReportFiles = Seq(successfulUpload)

  "UploadedReportFilesController" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.UploadedReportFilesController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UploadedReportFilesView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, uploadedReportFiles)(request, messages(application)).toString
      }
    }

    "must display every successful upload without imposing an application-level file limit" in {
      val uploads     = (1 to 100).map { index =>
        successfulUpload.copy(
          reference = s"successful-reference-$index",
          fileUploadDetails = Some(FileUploadDetails(s"return-$index.csv"))
        )
      }
      val application = applicationBuilder(
        monthlyReturn = Some(emptyMonthlyReturn.copy(fileUploads = uploads))
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.UploadedReportFilesController.onPageLoad().url)

        val result  = route(application, request).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include("You have added 100 report files")
        uploads.foreach { upload =>
          content must include(upload.fileUploadDetails.value.fileName)
        }
      }
    }

    "must redirect to Manage ISAs for a GET if no existing monthly return is found" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(GET, routes.UploadedReportFilesController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual manageIsasUrl(application)
      }
    }

    "must redirect to the next page when the user selects Yes" in {
      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn))
        .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.UploadedReportFilesController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when the user selects No" in {
      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn))
        .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.UploadedReportFilesController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> No.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return BadRequest and errors when no option is selected" in {
      val application = applicationBuilder(monthlyReturn = Some(monthlyReturn)).build()

      running(application) {
        val request = FakeRequest(POST, routes.UploadedReportFilesController.onSubmit().url)

        val boundForm = form.bind(Map.empty[String, String])
        val view      = application.injector.instanceOf[UploadedReportFilesView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, uploadedReportFiles)(request, messages(application)).toString
      }
    }
  }
}
