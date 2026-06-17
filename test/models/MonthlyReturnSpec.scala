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

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}

class MonthlyReturnSpec extends SpecBase {

  "MonthlyReturn" - {

    "must round-trip through JSON" in {
      val monthlyReturn = MonthlyReturn(
        submissionId = testSubmissionId,
        nilReturn = true
      )

      Json.toJson(monthlyReturn).validate[MonthlyReturn] mustEqual JsSuccess(monthlyReturn)
    }

    "must read the monthly return response returned by the backend" in {
      val json = Json.obj(
        "zReference"   -> testZReference,
        "submissionId" -> testSubmissionId,
        "taxYear"      -> testTaxYear,
        "month"        -> testMonth,
        "nilReturn"    -> true,
        "fileUploads"  -> Json.arr(),
        "createdOn"    -> "2026-03-15T12:00:00Z",
        "lastUpdated"  -> "2026-03-15T12:00:00Z"
      )

      json.validate[MonthlyReturn] mustEqual JsSuccess(
        MonthlyReturn(testSubmissionId, nilReturn = true)
      )
    }

    "must read successful file upload details returned by the backend" in {
      val json = Json.obj(
        "zReference"   -> testZReference,
        "submissionId" -> testSubmissionId,
        "taxYear"      -> testTaxYear,
        "month"        -> testMonth,
        "nilReturn"    -> false,
        "fileUploads"  -> Json.arr(
          Json.obj(
            "reference"         -> "successful-reference",
            "status"            -> FileUploadStatus.UpscanSuccess,
            "createdOn"         -> "2026-03-15T12:00:00Z",
            "fileUploadDetails" -> Json.obj(
              "fileName"          -> "return.csv",
              "fileMimeType"      -> "text/csv",
              "uploadTimestamp"   -> "2026-03-15T12:01:00Z",
              "checksum"          -> "checksum",
              "size"              -> 123,
              "upscanDownloadUrl" -> "https://example.com/file",
              "upscanCompletedOn" -> "2026-03-15T12:02:00Z"
            )
          )
        ),
        "declaredOn"   -> "2026-03-15T12:03:00Z",
        "createdOn"    -> "2026-03-15T12:00:00Z",
        "lastUpdated"  -> "2026-03-15T12:00:00Z"
      )

      json.validate[MonthlyReturn] mustEqual JsSuccess(
        MonthlyReturn(
          testSubmissionId,
          nilReturn = false,
          fileUploads = Seq(
            FileUpload(
              reference = "successful-reference",
              status = FileUploadStatus.UpscanSuccess,
              fileUploadDetails = Some(FileUploadDetails("return.csv"))
            )
          )
        )
      )
    }
  }

  "FileUpload.isSuccessful" - {

    "must be true when status is UPSCAN_SUCCESS" in {
      FileUpload("ref", FileUploadStatus.UpscanSuccess).isSuccessful mustEqual true
    }

    "must be true when status is VALIDATION_SUCCESS" in {
      FileUpload("ref", FileUploadStatus.ValidationSuccess).isSuccessful mustEqual true
    }

    "must be false for any other status" in {
      FileUpload("ref", "CREATED").isSuccessful mustEqual false
      FileUpload("ref", "VALIDATION_FAILURE").isSuccessful mustEqual false
      FileUpload("ref", "UPSCAN_QUARANTINE").isSuccessful mustEqual false
    }
  }

  "CreateMonthlyReturnResponse" - {

    "must read the create response returned by the backend" in {
      val json = Json.obj("submissionId" -> testSubmissionId)

      json.validate[CreateMonthlyReturnResponse] mustEqual JsSuccess(
        CreateMonthlyReturnResponse(testSubmissionId)
      )
    }
  }
}
