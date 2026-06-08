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
import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction, IdentifierAction}
import forms.MonthlyReportSubmissionFormProvider
import models.MonthlyReturn
import models.YesNoAnswer.{No, Yes}
import models.requests.IdentifierRequest
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, BodyParser, Call, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.StorageService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.MonthlyReportSubmissionView

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MonthlyReportSubmissionControllerSpec extends SpecBase with MockitoSugar {

  private val onwardRoute          = Call("GET", "/foo")
  private val existingSubmissionId = testSubmissionId

  private val formProvider = new MonthlyReportSubmissionFormProvider()
  private val form         = formProvider()

  private def applicationWith(
    monthlyReturn: Option[MonthlyReturn] = None,
    storageService: StorageService = mockStorageService()
  ) =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(monthlyReturn)),
        bind[IdentifierAction].toInstance(new TestIdentifierAction),
        bind[java.time.Clock].toInstance(testReportingWindowClock),
        bind[StorageService].toInstance(storageService),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

  private class TestIdentifierAction extends IdentifierAction {

    override def parser: BodyParser[AnyContent] =
      stubControllerComponents().parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext =
      scala.concurrent.ExecutionContext.Implicits.global

    override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
      block(IdentifierRequest(request, testZReference))
  }

  private def mockStorageService(): StorageService = {
    val storageService = mock[StorageService]
    when(
      storageService.saveForThisWindow(
        any[String],
        any[Option[MonthlyReturn]],
        any[Boolean]
      )(any[HeaderCarrier])
    )
      .thenReturn(Future.successful(uploadReportMonthlyReturn()))
    storageService
  }

  private def nilReturnMonthlyReturn(submissionId: UUID = existingSubmissionId): MonthlyReturn =
    MonthlyReturn(submissionId = submissionId, nilReturn = true)

  private def uploadReportMonthlyReturn(submissionId: UUID = existingSubmissionId): MonthlyReturn =
    MonthlyReturn(submissionId = submissionId, nilReturn = false)

  "MonthlyReportSubmissionController" - {

    "must return OK and the empty view for a GET when there is no existing answer" in {
      val app = applicationWith()

      running(app) {
        val request = FakeRequest(GET, routes.MonthlyReportSubmissionController.onPageLoad().url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[MonthlyReportSubmissionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(app)).toString
      }
    }

    "must prepopulate No for a GET when backend storage has a nil report" in {
      val app = applicationWith(monthlyReturn = Some(nilReturnMonthlyReturn()))

      running(app) {
        val request = FakeRequest(GET, routes.MonthlyReportSubmissionController.onPageLoad().url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[MonthlyReportSubmissionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(No))(request, messages(app)).toString
      }
    }

    "must prepopulate Yes for a GET when backend storage has an upload report" in {
      val app = applicationWith(monthlyReturn = Some(uploadReportMonthlyReturn()))

      running(app) {
        val request = FakeRequest(GET, routes.MonthlyReportSubmissionController.onPageLoad().url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[MonthlyReportSubmissionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Yes))(request, messages(app)).toString
      }
    }

    "must return BadRequest and errors when no option is selected" in {
      val storageService = mockStorageService()
      val app            = applicationWith(storageService = storageService)

      running(app) {
        val request = FakeRequest(POST, routes.MonthlyReportSubmissionController.onSubmit().url)

        val boundForm = form.bind(Map.empty[String, String])
        val view      = app.injector.instanceOf[MonthlyReportSubmissionView]

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(app)).toString
        verify(storageService, never()).saveForThisWindow(
          any[String],
          any[Option[MonthlyReturn]],
          any[Boolean]
        )(
          any[HeaderCarrier]
        )
      }
    }

    "must save a new nil return and redirect to the next page" in {
      val storageService = mockStorageService()
      val app            = applicationWith(storageService = storageService)

      running(app) {
        val request =
          FakeRequest(POST, routes.MonthlyReportSubmissionController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> No.toString)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER

        verify(storageService).saveForThisWindow(eqTo(testZReference), eqTo(None), eqTo(true))(any[HeaderCarrier])
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must save an updated answer against the existing monthly return and redirect to the next page" in {
      val storageService = mockStorageService()
      val existing       = nilReturnMonthlyReturn()
      val app            = applicationWith(
        monthlyReturn = Some(existing),
        storageService = storageService
      )

      running(app) {
        val request =
          FakeRequest(POST, routes.MonthlyReportSubmissionController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> Yes.toString)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER

        verify(storageService).saveForThisWindow(eqTo(testZReference), eqTo(Some(existing)), eqTo(false))(
          any[HeaderCarrier]
        )
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return InternalServerError when the backend save fails" in {
      val storageService = mock[StorageService]
      when(
        storageService.saveForThisWindow(
          any[String],
          any[Option[MonthlyReturn]],
          any[Boolean]
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val app = applicationWith(storageService = storageService)

      running(app) {
        val request =
          FakeRequest(POST, routes.MonthlyReportSubmissionController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> No.toString)

        val result = route(app, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
