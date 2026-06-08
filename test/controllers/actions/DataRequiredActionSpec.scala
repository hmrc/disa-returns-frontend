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

package controllers.actions

import base.SpecBase
import controllers.routes
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRequiredActionSpec extends SpecBase {

  class Harness extends DataRequiredActionImpl {
    def callRefine[A](request: OptionalDataRequest[A]): Future[Either[play.api.mvc.Result, DataRequest[A]]] = refine(
      request
    )
  }

  "Data Required Action" - {

    "must redirect to Journey Recovery when there is no monthly return" in {
      val action = new Harness

      val result =
        action.callRefine(OptionalDataRequest(FakeRequest(), testZReference, None)).futureValue

      result.left.value.header.status mustEqual SEE_OTHER
      result.left.value.header.headers(LOCATION) mustEqual routes.JourneyRecoveryController.onPageLoad().url
    }

    "must build a DataRequest when there is a monthly return" in {
      val action = new Harness

      val result = action
        .callRefine(
          OptionalDataRequest(FakeRequest(), testZReference, Some(emptyMonthlyReturn))
        )
        .futureValue

      result.value.zReference mustEqual testZReference
      result.value.monthlyReturn mustEqual emptyMonthlyReturn
    }
  }
}
