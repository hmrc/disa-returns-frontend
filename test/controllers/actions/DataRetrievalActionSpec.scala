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

package controllers.actions

import base.SpecBase
import models.requests.{IdentifierRequest, OptionalDataRequest}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.StorageService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(storageService: StorageService) extends DataRetrievalActionImpl(storageService) {
    def callTransform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no monthly return in backend storage" - {

      "must set monthlyReturn to None in the request" in {

        val storageService = mock[StorageService]
        when(storageService.retrieveForThisWindow(eqTo(testZReference))(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))
        val action         = new Harness(storageService)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), testZReference)).futureValue

        result.zReference mustEqual testZReference
        result.monthlyReturn must not be defined
      }
    }

    "when there is a monthly return in backend storage" - {

      "must add it to the request" in {

        val storageService = mock[StorageService]
        when(storageService.retrieveForThisWindow(eqTo(testZReference))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(emptyMonthlyReturn)))
        val action         = new Harness(storageService)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), testZReference)).futureValue

        result.zReference mustEqual testZReference
        result.monthlyReturn.value mustEqual emptyMonthlyReturn
      }
    }
  }
}
