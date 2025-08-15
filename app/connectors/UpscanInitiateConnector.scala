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

package connectors

import com.google.inject.Inject
import models.UploadId
import models.upscan.*
import play.api.http.HeaderNames
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*
import play.api.libs.ws.writeableOf_JsValue

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanInitiateConnector @Inject()(httpClient: HttpClientV2)(implicit
                                                                  val ec: ExecutionContext
) {
  private val callbackUrl = "http://localhost:1205/disa-returns-frontend/upscan-callback"
  private val headers = Seq(
    HeaderNames.CONTENT_TYPE -> "application/json"
  )
  private val initiateUrl = "http://localhost:9570/upscan/v2/initiate"

  def initiateRequest(redirectOnSuccess: Option[String],
                      redirectOnError  : Option[String])(implicit hc: HeaderCarrier): Future[PreparedUpload] = {
    val request = UpscanInitiateRequest(
      callbackUrl = callbackUrl,
      successRedirect = redirectOnSuccess,
      errorRedirect = redirectOnError,
      minimumFileSize = None,
      maximumFileSize = None
    )

    httpClient
      .post(url"$initiateUrl")
      .setHeader(headers: _*)
      .withBody(Json.toJson(request))
      .execute[PreparedUpload]
  }
}
