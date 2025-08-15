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

package service

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

import com.google.inject.Inject
import connectors.UpscanInitiateConnector
import models.UploadId
import models.upscan.{UpscanFileReference, UpscanInitiateResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanService @Inject() (
  upscanConnector: UpscanInitiateConnector
)(implicit ec: ExecutionContext) {
  def upscanInitiate(redirectOnSuccess: Option[String],
                     redirectOnError  : Option[String])(implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] =
    upscanConnector.initiateRequest(redirectOnSuccess,redirectOnError).map { response =>
      UpscanInitiateResponse(
        fileReference = UpscanFileReference(response.reference),
        postTarget = response.uploadRequest.href,
        formFields = response.uploadRequest.fields
      )
    }
}
