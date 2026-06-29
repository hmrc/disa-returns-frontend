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

object FileValidationErrorCodes {

  private val columnByCode: Map[String, Option[String]] = Map(
    "E010" -> Some("A"),
    "E011" -> Some("A"),
    "E020" -> Some("B"),
    "E021" -> Some("B"),
    "E022" -> Some("B"),
    "E023" -> Some("B"),
    "E024" -> Some("B"),
    "E030" -> Some("C"),
    "E031" -> Some("C"),
    "E032" -> Some("C"),
    "E040" -> Some("D"),
    "E041" -> Some("D"),
    "E050" -> Some("E"),
    "E051" -> Some("E"),
    "E052" -> Some("E"),
    "E060" -> Some("F"),
    "E061" -> Some("F"),
    "E062" -> Some("F"),
    "E070" -> Some("G"),
    "E071" -> Some("G"),
    "E080" -> Some("H"),
    "E081" -> Some("H"),
    "E090" -> Some("I"),
    "E091" -> Some("I"),
    "E092" -> Some("I"),
    "E100" -> Some("J"),
    "E101" -> Some("J"),
    "E102" -> Some("J"),
    "E110" -> Some("K"),
    "E111" -> Some("K"),
    "E112" -> Some("K"),
    "E120" -> Some("L"),
    "E121" -> Some("L"),
    "E122" -> Some("L"),
    "E123" -> Some("L"),
    "E130" -> Some("M"),
    "E131" -> Some("M"),
    "E132" -> Some("M"),
    "E140" -> Some("N"),
    "E141" -> Some("N"),
    "E142" -> Some("N"),
    "E150" -> Some("O"),
    "E151" -> Some("O"),
    "E152" -> Some("O"),
    "E160" -> Some("P"),
    "E161" -> Some("P"),
    "E162" -> Some("P"),
    "E170" -> Some("Q"),
    "E171" -> Some("Q"),
    "E172" -> Some("Q"),
    "E180" -> Some("R"),
    "E181" -> Some("R"),
    "E182" -> Some("R"),
    "E190" -> Some("S"),
    "E191" -> Some("S"),
    "E192" -> Some("S")
  )

  def cellReference(errorCode: String, rowNumber: Int): String =
    columnByCode.get(errorCode).flatten match {
      case Some(column) => s"$column$rowNumber"
      case None         => s"Row $rowNumber"
    }

  def messageKey(errorCode: String): String =
    s"fileValidationErrors.$errorCode"
}
