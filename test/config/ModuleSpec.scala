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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import services.{ReportingContextSource, SystemReportingContextSource}
import testOnly.TestOnlyBackendReportingContextSource

class ModuleSpec extends SpecBase {
  "Module" - {
    "must bind the backend reporting context source when test-only routes are enabled" in {
      val application = new GuiceApplicationBuilder()
        .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        application.injector.instanceOf[ReportingContextSource] mustBe a[TestOnlyBackendReportingContextSource]
      }
    }

    "must bind the backend-authoritative system reporting context source otherwise" in {
      val application = new GuiceApplicationBuilder().build()

      running(application) {
        application.injector.instanceOf[ReportingContextSource] mustBe a[SystemReportingContextSource]
      }
    }
  }
}
