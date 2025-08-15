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

package models.upscan

import play.api.libs.json.*

import java.net.URL
import java.time.Instant

sealed trait CallbackBody {
  def reference:String
}

case class ReadyCallbackBody(
                              reference    : String,
                              downloadUrl  : URL,
                              uploadDetails: UploadDetails
                            ) extends CallbackBody

case class FailedCallbackBody(
                               reference: String,
                               failureDetails: ErrorDetails
                             ) extends CallbackBody

case class UploadDetails(
                          uploadTimestamp: Instant,
                          checksum       : String,
                          fileMimeType   : String,
                          fileName       : String,
                          size           : Long
                        )

case class ErrorDetails(
                         failureReason: String,
                         message      : String
                       )

object CallbackBody {
  implicit val uploadDetailsFormat: OFormat[UploadDetails] = Json.format[UploadDetails]
  implicit val errorDetailsFormat: OFormat[ErrorDetails] = Json.format[ErrorDetails]
  implicit val urlFormat: Format[URL] = Format(
    Reads.StringReads.map(new URL(_)),
    Writes[URL](url => JsString(url.toString))
  )
  implicit val readyCallbackBodyFormat: OFormat[ReadyCallbackBody] = Json.format[ReadyCallbackBody]
  implicit val failedCallbackBodyFormat: OFormat[FailedCallbackBody] = Json.format[FailedCallbackBody]
  
  implicit val callbackReads:Reads[CallbackBody] = (json: JsValue) => json \ "fileStatus" match {
    case JsDefined(JsString("READY")) => json.validate[ReadyCallbackBody]
    case JsDefined(JsString("FAILED")) => json.validate[FailedCallbackBody]
    case JsDefined(value)              => JsError(s"Invalid type discriminator: $value")
    case _                             => JsError(s"Missing type discriminator")
  }

}