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

package views

import base.SpecBase
import controllers.routes
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import views.html.FileProcessingView

class FileProcessingViewSpec extends SpecBase {

  private val testReference = "test-reference"

  "FileProcessingView" - {

    "must render the spinner, copy and the server-side polling destination for the given reference" in {

      val application = applicationBuilder().build()

      running(application) {
        val view = application.injector.instanceOf[FileProcessingView]

        implicit val request: FakeRequest[_] = FakeRequest()
        implicit val msgs                    = messages(application)

        val statusUrl = routes.FileProcessingController.status(testReference).url
        val failedUrl = routes.FileUploadErrorController.fileUploadFailed().url

        val html = view(testReference)(request, msgs).body

        html must include("""id="file-processing"""")
        html must include("""class="ccms-loader govuk-!-margin-bottom-4"""")
        html must include(msgs("fileProcessing.heading"))
        html must include(msgs("fileProcessing.body"))
        html must include(s"""data-status-url="$statusUrl"""")
        html must include(s"""data-failed-url="$failedUrl"""")
        html must not include "data-success-url"
        html must not include "data-validation-errors-url"
        html must include(s"""data-poll-interval-millis="$testFileProcessingPollIntervalMillis"""")
        html must include(s"""data-max-wait-minutes="$testFileProcessingMaxWaitMinutes"""")
        html must include("javascripts/fileProcessing.js")
      }
    }
  }
}
