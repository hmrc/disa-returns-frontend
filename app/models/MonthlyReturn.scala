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

package models

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Json, OFormat, Reads, Writes}

import java.util.UUID
import scala.util.Try

case class MonthlyReturn(
  submissionId: UUID,
  nilReturn: Boolean,
  fileUploads: Seq[FileUpload] = Seq.empty
)

case class CreateMonthlyReturnResponse(
  submissionId: UUID
)

case class MonthlyReturnSaveResult(
  monthlyReturn: MonthlyReturn,
  created: Boolean
)

case class FileUpload(
  reference: String,
  status: String,
  fileUploadDetails: Option[FileUploadDetails] = None
) {

  def isSuccessful: Boolean =
    status == FileUploadStatus.UpscanSuccess || status == FileUploadStatus.ValidationSuccess
}

case class FileUploadDetails(
  fileName: String,
  validation: Option[ValidationResult] = None
)

case class ValidationResult(
  rowsValidated: Int,
  validationErrors: Int,
  status: String,
  inlineErrors: Seq[InlineError] = Seq.empty
)

case class InlineError(
  rowNumber: Int,
  errorCodes: Seq[String]
)

object FileUploadStatus {
  val UpscanSuccess: String     = "UPSCAN_SUCCESS"
  val ValidationSuccess: String = "VALIDATION_SUCCESS"
}

object MonthlyReturn {

  implicit val uuidFormat: Format[UUID] =
    Format(
      Reads {
        case JsString(value) =>
          Try(UUID.fromString(value))
            .fold(_ => JsError("error.expected.uuid"), JsSuccess(_))
        case _               => JsError("error.expected.uuid")
      },
      Writes(uuid => JsString(uuid.toString))
    )

  implicit val inlineErrorFormat: OFormat[InlineError] =
    Json.format[InlineError]

  implicit val validationResultFormat: OFormat[ValidationResult] =
    Json.using[Json.WithDefaultValues].format[ValidationResult]

  implicit val fileUploadDetailsFormat: OFormat[FileUploadDetails] =
    Json.using[Json.WithDefaultValues].format[FileUploadDetails]

  implicit val fileUploadFormat: OFormat[FileUpload] =
    Json.using[Json.WithDefaultValues].format[FileUpload]

  implicit val format: OFormat[MonthlyReturn] =
    Json.using[Json.WithDefaultValues].format[MonthlyReturn]
}

object CreateMonthlyReturnResponse {

  import models.MonthlyReturn.uuidFormat

  implicit val format: OFormat[CreateMonthlyReturnResponse] =
    Json.format[CreateMonthlyReturnResponse]
}
