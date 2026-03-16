package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.DownloadReportTemplateView

class DownloadReportTemplateControllerSpec extends SpecBase {

  "DownloadReportTemplateController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.DownloadReportTemplateController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DownloadReportTemplateView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
