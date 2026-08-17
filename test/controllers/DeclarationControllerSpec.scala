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
import models.MonthlyReturnDeclarationResult
import models.MonthlyReturnDeclarationResult.{AlreadyDeclared, Declared, Failed, MonthlyReturnNotFound, OutsideDeclarationPeriod}
import models.{FileUpload, FileUploadDetails, FileUploadStatus}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.StorageService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.DeclarationView
import viewmodels.DeclarationViewModel

import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar {

  private val successfulUpload = FileUpload(
    reference = "successful-reference",
    status = FileUploadStatus.ValidationSuccess,
    fileUploadDetails = Some(FileUploadDetails("return.csv"))
  )

  private val nonNilReturnWithFile = emptyMonthlyReturn.copy(fileUploads = Seq(successfulUpload))
  private val nilReturn            = emptyMonthlyReturn.copy(nilReturn = true)

  "DeclarationController" - {

    "must return OK and the correct view when nilReturn is false" in {

      val application =
        applicationBuilder(monthlyReturn = Some(nonNilReturnWithFile)).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.DeclarationController.onPageLoad().url)

        val result =
          route(application, request).value

        val view =
          application.injector.instanceOf[DeclarationView]

        val expectedView =
          view(DeclarationViewModel(false))(request, messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
      }
    }

    "must return OK and the correct view when nilReturn is true" in {

      val application =
        applicationBuilder(monthlyReturn = Some(nilReturn)).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.DeclarationController.onPageLoad().url)

        val result =
          route(application, request).value

        val view =
          application.injector.instanceOf[DeclarationView]

        val expectedView =
          view(DeclarationViewModel(true))(request, messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
      }
    }

    "must redirect to SubmissionComplete when the declaration is submitted successfully" in {
      val storageService = mock[StorageService]
      when(storageService.declareForThisPeriod(eqTo(testZReference))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Declared))

      val application =
        applicationBuilder(monthlyReturn = Some(nilReturn))
          .overrides(bind[StorageService].toInstance(storageService))
          .build()

      running(application) {
        val request = FakeRequest(POST, routes.DeclarationController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SubmissionCompleteController.onPageLoad().url
        verify(storageService).declareForThisPeriod(eqTo(testZReference))(any[HeaderCarrier])
      }
    }

    Seq[(MonthlyReturnDeclarationResult, String)](
      AlreadyDeclared          -> "the return is already declared",
      OutsideDeclarationPeriod -> "the declaration period is closed",
      MonthlyReturnNotFound    -> "the monthly return is missing",
      Failed                   -> "the declaration fails"
    ).foreach { case (outcome, description) =>
      s"must render the shared internal server error page when $description" in {
        val storageService = mock[StorageService]
        when(storageService.declareForThisPeriod(eqTo(testZReference))(any[HeaderCarrier]))
          .thenReturn(Future.successful(outcome))

        val application = applicationBuilder(monthlyReturn = Some(nilReturn))
          .overrides(bind[StorageService].toInstance(storageService))
          .build()

        running(application) {
          val result = route(application, FakeRequest(POST, routes.DeclarationController.onSubmit().url)).value

          status(result) mustEqual INTERNAL_SERVER_ERROR
          contentAsString(result) must not be empty
        }
      }
    }

    "must redirect to Manage ISAs for a GET if no existing data is found" in {
      val application = applicationBuilder(monthlyReturn = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.DeclarationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual manageIsasUrl(application)
      }
    }

    "must redirect to Manage ISAs for a POST if no existing data is found" in {
      val application = applicationBuilder(monthlyReturn = None).build()

      running(application) {
        val request = FakeRequest(POST, routes.DeclarationController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual manageIsasUrl(application)
      }
    }

    "must prevent a non-nil return without a valid file from reaching declaration" in {
      val application = applicationBuilder(monthlyReturn = Some(emptyMonthlyReturn)).build()

      running(application) {
        Seq(GET, POST).foreach { method =>
          val result = route(application, FakeRequest(method, routes.DeclarationController.onPageLoad().url)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual manageIsasUrl(application)
        }
      }
    }
  }
}
