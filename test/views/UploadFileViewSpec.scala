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
import models.upscan.{UploadRequest, UpscanInitiateResponse}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import views.html.UploadFileView
import viewmodels.UploadViewModel

class UploadFileViewSpec extends SpecBase {

  private val upscanResponse = UpscanInitiateResponse(
    reference = "test-reference",
    uploadRequest = UploadRequest(
      href = "https://upscan/upload",
      fields = Map("key" -> "mock-key")
    )
  )

  "UploadFileView" - {

    "must render the visible content and the initially-hidden upload-in-progress panel" in {

      val application = applicationBuilder().build()

      running(application) {
        val view = application.injector.instanceOf[UploadFileView]

        implicit val request: FakeRequest[_] = FakeRequest()
        implicit val msgs                    = messages(application)

        val errorRedirectUrl = routes.UploadFileController.onError().url
        val emptyFileUrl     = routes.FileUploadErrorController.emptyFileUploaded().url

        val html = view(UploadViewModel(upscan = upscanResponse, error = None))(request, msgs).body

        html must include("""id="upload-file-content"""")
        html must include("""id="upload-in-progress"""")
        html must include("""id="upload-in-progress-heading"""")
        html must include("""hidden="hidden"""")
        html must include("""class="ccms-loader govuk-!-margin-bottom-4"""")
        html must include(msgs("uploadFile.uploading.heading"))
        html must include(msgs("uploadFile.uploading.body"))
        html must include(s"""data-error-redirect="$errorRedirectUrl"""")
        html must include(s"""data-empty-file-url="$emptyFileUrl"""")
        html must include(s"""data-min-file-size="$testUpscanMinFileSize"""")
        html must include(s"""data-max-file-size="$testUpscanMaxFileSize"""")
        html must include("""data-accepted-extensions=".csv,.xlsx"""")
        html must include("""id="upload-live-region"""")
        html must include("""aria-live="polite"""")
        html must include("javascripts/uploadFile.js")
        html must include(routes.DownloadReportTemplateController.download().url)
      }
    }

    "must render the real error summary and file error message when model.error is set" in {

      val application = applicationBuilder().build()

      running(application) {
        val view = application.injector.instanceOf[UploadFileView]

        implicit val request: FakeRequest[_] = FakeRequest()
        implicit val msgs                    = messages(application)

        val html =
          view(UploadViewModel(upscan = upscanResponse, error = Some("uploadFile.rejected")))(request, msgs).body

        html must include("""class="govuk-error-summary"""")
        html must include(msgs("uploadFile.rejected"))
        html must include("""id="file-error"""")
        html must include("""govuk-file-upload--error""")
      }
    }
  }
}
