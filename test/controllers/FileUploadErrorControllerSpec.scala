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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.{DuplicateFileUploadView, EmptyUploadedFileView, FileContainsVirusView, FilePasswordProtectedView, FileUploadFailedView, InvalidFileTypeView}

class FileUploadErrorControllerSpec extends SpecBase {

  "FileUploadErrorController" - {

    "filePasswordProtected must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.FileUploadErrorController.filePasswordProtected().url
          )

        val result = route(application, request).value

        val view =
          application.injector.instanceOf[FilePasswordProtectedView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view()(request, messages(application)).toString
      }
    }

    "fileContainsVirus must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.FileUploadErrorController.fileContainsVirus().url
          )

        val result = route(application, request).value

        val view =
          application.injector.instanceOf[FileContainsVirusView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view()(request, messages(application)).toString
      }
    }

    "emptyFileUploaded must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.FileUploadErrorController.emptyFileUploaded().url
          )

        val result = route(application, request).value

        val view =
          application.injector.instanceOf[EmptyUploadedFileView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view()(request, messages(application)).toString
      }
    }

    "invalidFileType must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.FileUploadErrorController.invalidFileType().url
          )

        val result = route(application, request).value

        val view =
          application.injector.instanceOf[InvalidFileTypeView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view()(request, messages(application)).toString
      }
    }

    "duplicateFileUpload must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.FileUploadErrorController.duplicateFileUpload().url
          )

        val result = route(application, request).value

        val view =
          application.injector.instanceOf[DuplicateFileUploadView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view()(request, messages(application)).toString
      }
    }

    "fileUploadFailed must return OK and the correct view for a GET" in {

      val application = applicationBuilder().build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.FileUploadErrorController.fileUploadFailed().url
          )

        val result = route(application, request).value

        val view =
          application.injector.instanceOf[FileUploadFailedView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view()(request, messages(application)).toString
      }
    }
  }
}
