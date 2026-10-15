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
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import play.api.test.Helpers.running

import scala.jdk.CollectionConverters._

class FileValidationErrorCodesSpec extends SpecBase {

  "FileValidationErrorCodes" - {

    "genericMessageKey must identify the safe fallback message" in {
      FileValidationErrorCodes.genericMessageKey mustEqual "fileValidationErrors.generic"
    }

    "cellReference" - {

      "must return the column letter and row number for known error codes" in {
        FileValidationErrorCodes.cellReference("E010", 3) mustEqual "A3"
        FileValidationErrorCodes.cellReference("E020", 1) mustEqual "B1"
        FileValidationErrorCodes.cellReference("E030", 5) mustEqual "C5"
        FileValidationErrorCodes.cellReference("E040", 2) mustEqual "D2"
        FileValidationErrorCodes.cellReference("E050", 4) mustEqual "E4"
        FileValidationErrorCodes.cellReference("E060", 1) mustEqual "F1"
        FileValidationErrorCodes.cellReference("E070", 2) mustEqual "G2"
        FileValidationErrorCodes.cellReference("E080", 3) mustEqual "H3"
        FileValidationErrorCodes.cellReference("E090", 1) mustEqual "I1"
        FileValidationErrorCodes.cellReference("E100", 2) mustEqual "J2"
        FileValidationErrorCodes.cellReference("E110", 1) mustEqual "K1"
        FileValidationErrorCodes.cellReference("E120", 3) mustEqual "L3"
        FileValidationErrorCodes.cellReference("E130", 4) mustEqual "M4"
        FileValidationErrorCodes.cellReference("E140", 1) mustEqual "N1"
        FileValidationErrorCodes.cellReference("E150", 2) mustEqual "O2"
        FileValidationErrorCodes.cellReference("E160", 5) mustEqual "P5"
        FileValidationErrorCodes.cellReference("E170", 1) mustEqual "Q1"
        FileValidationErrorCodes.cellReference("E180", 2) mustEqual "R2"
        FileValidationErrorCodes.cellReference("E190", 3) mustEqual "S3"
      }

      "must use the row number in the cell reference" in {
        FileValidationErrorCodes.cellReference("E010", 1) mustEqual "A1"
        FileValidationErrorCodes.cellReference("E010", 10) mustEqual "A10"
      }

      "must return a row-only reference for an unknown error code" in {
        FileValidationErrorCodes.cellReference("E999", 4) mustEqual "Row 4"
      }

      "must return a row-only reference for E001 since it is not mapped to a column" in {
        FileValidationErrorCodes.cellReference("E001", 2) mustEqual "Row 2"
      }
    }

    "messageKey" - {

      "must return the i18n message key for a known error code" in {
        val application = applicationBuilder().build()

        running(application) {
          implicit val implicitMessages = messages(application)

          FileValidationErrorCodes.messageKey("E001") mustEqual "fileValidationErrors.E001"
          FileValidationErrorCodes.messageKey("E010") mustEqual "fileValidationErrors.E010"
          FileValidationErrorCodes.messageKey("E192") mustEqual "fileValidationErrors.E192"
        }
      }

      "must return the generic key for an unknown error code" in {
        val application = applicationBuilder().build()

        running(application) {
          implicit val implicitMessages = messages(application)

          FileValidationErrorCodes.messageKey("E999") mustEqual FileValidationErrorCodes.genericMessageKey
        }
      }
    }

    "warnForUnknownCodes" - {

      "must log one warning per distinct unknown code and upload reference" in {
        val application   = applicationBuilder().build()
        val logger        = LoggerFactory
          .getLogger(FileValidationErrorCodes.getClass.getName.stripSuffix("$"))
          .asInstanceOf[Logger]
        val originalLevel = logger.getLevel
        val appender      = new ListAppender[ILoggingEvent]()
        val unknownCode   = "E999"
        val otherCode     = "E998"
        val reference     = "test-file-reference"
        appender.start()
        logger.addAppender(appender)
        logger.setLevel(Level.WARN)

        try
          running(application) {
            implicit val implicitMessages = messages(application)

            FileValidationErrorCodes.warnForUnknownCodes(
              Seq(unknownCode, "E010", unknownCode, otherCode, otherCode),
              reference
            )

            val warnings = appender.list.asScala.filter(_.getLevel == Level.WARN)
            warnings.count(_.getFormattedMessage.contains(unknownCode)) mustEqual 1
            warnings.count(_.getFormattedMessage.contains(otherCode)) mustEqual 1
            warnings.count(_.getFormattedMessage.contains(reference)) mustEqual 2
          }
        finally {
          logger.detachAppender(appender)
          logger.setLevel(originalLevel)
          appender.stop()
        }
      }
    }
  }
}
