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

import models.YesNoAnswer.{No, Yes}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.{DefaultMessagesApi, Lang, Messages, MessagesImpl}
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

class YesNoAnswerSpec extends AnyFreeSpec with Matchers with Enumerable.Implicits {

  "YesNoAnswer" - {

    "must serialise to yes/no strings" in {
      Json.toJson[YesNoAnswer](Yes) mustEqual JsString("yes")
      Json.toJson[YesNoAnswer](No) mustEqual JsString("no")
    }

    "must deserialise yes/no strings" in {
      JsString("yes").as[YesNoAnswer] mustEqual Yes
      JsString("no").as[YesNoAnswer] mustEqual No
    }

    "must use plain yes/no labels when no message key prefix is supplied" in {
      val messagesApi = new DefaultMessagesApi(
        Map(
          "en" -> Map(
            "site.yes" -> "Yes",
            "site.no"  -> "No"
          )
        )
      )

      implicit val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

      YesNoAnswer.options().map(_.content) mustEqual Seq(Text("Yes"), Text("No"))
    }
  }
}
