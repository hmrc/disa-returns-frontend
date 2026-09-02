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

package config

import base.SpecBase
import play.api.test.Helpers.running

class FrontendAppConfigSpec extends SpecBase {

  "FrontendAppConfig" - {

    "must compose the Manage ISAs URL from the accounts service and its route context" in {
      val application = applicationBuilder().build()

      running(application) {
        application.injector.instanceOf[FrontendAppConfig].manageIsasUrl mustEqual
          "https://manage-isas.example.com:443/obligations/account/isa"
      }
    }

  }
}
