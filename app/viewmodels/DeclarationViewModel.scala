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

package viewmodels

final case class DeclarationViewModel(
  isNilReturn: Boolean
) {

  val titleKey: String =
    if (isNilReturn) "declaration.title.nilReturn"
    else "declaration.title"

  val headingKey: String =
    if (isNilReturn) "declaration.heading.nilReturn"
    else "declaration.heading"

  val introKey: String =
    "declaration.p1"

  val bulletKeys: Seq[String] =
    Seq(
      "declaration.li1",
      "declaration.li2",
      "declaration.li3"
    )

  val extraNilReturnParagraphKeys: Seq[String] =
    if (isNilReturn)
      Seq(
        "declaration.p2.nilReturn",
        "declaration.p3.nilReturn"
      )
    else
      Seq.empty

  val closingKey: String =
    "declaration.p4"

  val buttonKey: String =
    "declaration.button"
}
