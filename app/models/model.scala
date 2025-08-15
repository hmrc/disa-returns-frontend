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

package models

import org.bson.types.ObjectId
import play.api.libs.json.{Format, JsError, JsObject, JsString, JsSuccess, Json, OFormat, Reads, Writes, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

import java.net.URL
import java.util.UUID

sealed trait UploadStatus

object UploadStatus {
  case object InProgress extends UploadStatus

  case object Failed extends UploadStatus

  case class UploadedSuccessfully(
                                   name: String,
                                   mimeType: String,
                                   downloadUrl: URL,
                                   size: Option[Long],
                                   checksum: String
                                 ) extends UploadStatus

  implicit val urlFormat: Format[URL] = Format(
    Reads.StringReads.map(new URL(_)),
    Writes[URL](url => JsString(url.toString))
  )
  implicit val uploadedSuccessfullyReads: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]

  implicit val uploadStatusReads: Reads[UploadStatus] = Reads { json =>
    (json \ "_type").validate[String].flatMap {
      case "InProgress" => JsSuccess(InProgress)
      case "Failed" => JsSuccess(Failed)
      case "UploadedSuccessfully" =>
        uploadedSuccessfullyReads.reads(json)
      case other =>
        JsError(s"Unknown UploadStatus: $other")
    }
  }

  implicit val write: Writes[UploadStatus] =
    case UploadStatus.InProgress => JsObject(Map("_type" -> JsString("InProgress")))
    case UploadStatus.Failed => JsObject(Map("_type" -> JsString("Failed")))
    case s: UploadStatus.UploadedSuccessfully => Json.toJson(s).as[JsObject]
      + ("_type" -> JsString("UploadedSuccessfully"))
}


case class UploadDetails(
                          id: ObjectId,
                          uploadId: UploadId,
                          reference: String,
                          status: UploadStatus,
                          isaManagerReference:String

                        )

object UploadDetails {
  implicit val objectId: Format[ObjectId] = MongoFormats.objectIdFormat
  implicit val uploadIdFormat: Format[UploadId] = Format(
    Reads.StringReads.map(UploadId.apply),
    Writes[UploadId](uid => JsString(uid.value))
  )
  implicit val format: OFormat[UploadDetails] = Json.format[UploadDetails]
}

case class UploadId(value: String) extends AnyVal

object UploadId:
  def generate(): UploadId =
    UploadId(UUID.randomUUID().toString)


