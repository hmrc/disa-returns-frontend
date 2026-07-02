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
import views.html.SubmissionCompleteView

class SubmissionCompleteControllerSpec extends SpecBase {

  "SubmissionCompleteController" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(monthlyReturn = Some(emptyMonthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionCompleteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must display the submission complete content" in {
      val application = applicationBuilder(monthlyReturn = Some(emptyMonthlyReturn)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionCompleteController.onPageLoad().url)

        val result  = route(application, request).value
        val content = contentAsString(result)

        status(result) mustEqual OK
        content must include("Submission complete")
        content must include("We have sent you a confirmation email.")
        content must include("What happens next")
        content must include("If you need to make changes to your monthly report")
        content must include("If you need help")
        content must include("What did you think of this service?")
        content must include("/contact/beta-feedback?service=disa-returns-frontend")
        content must include(
          "backUrl=http://localhost:1205/obligations/returns/isa/obligations/returns/isa/submission-complete"
        )

        content must include("Return to manage reports")
        content must include(routes.IndexController.onPageLoad().url)
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(monthlyReturn = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionCompleteController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
