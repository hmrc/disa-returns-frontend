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

package testOnly.connectors

import base.ISpecBase
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import testOnly.forms.TestOnlyReportingOverrides
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class TestOnlyReportingOverridesConnectorISpec extends ISpecBase with BeforeAndAfterAll with BeforeAndAfterEach {
  private val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())
  private val path           = s"/disa-returns-backend/test-only/overrides/$testZReference"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    wireMockServer.resetAll()
    super.beforeEach()
  }

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.disa-returns-backend.protocol" -> "http",
        "microservice.services.disa-returns-backend.host"     -> "localhost",
        "microservice.services.disa-returns-backend.port"     -> wireMockServer.port()
      )
      .build()

  "TestOnlyReportingOverridesConnector.get" - {
    "must get the typed aggregate overrides exactly once" in {
      wireMockServer.stubFor(
        get(urlEqualTo(path)).willReturn(
          okJson(
            s"""{"zReference":"$testZReference","clock":{"date":"2026-06-17"},"reportingWindow":{"startDate":"2026-06-06T00:00:00Z","endDate":"2026-06-19T23:59:59.999999999Z"}}"""
          )
        )
      )

      val app = application
      running(app) {
        val current = app.injector
          .instanceOf[TestOnlyReportingOverridesConnector]
          .get(testZReference)(HeaderCarrier())
          .futureValue

        current.systemDate mustBe Some(LocalDate.parse("2026-06-17"))
        current.reportingWindow mustBe Some(
          LocalDate.parse("2026-06-06") -> LocalDate.parse("2026-06-19")
        )
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(path)))
      }
    }

    "must map null clock and reporting-window overrides to None" in {
      wireMockServer.stubFor(
        get(urlEqualTo(path)).willReturn(
          okJson(
            s"""{"zReference":"$testZReference","clock":null,"reportingWindow":null}"""
          )
        )
      )

      val app = application
      running(app) {
        val current = app.injector
          .instanceOf[TestOnlyReportingOverridesConnector]
          .get(testZReference)(HeaderCarrier())
          .futureValue

        current.systemDate mustBe None
        current.reportingWindow mustBe None
      }
    }
  }

  "TestOnlyReportingOverridesConnector.set" - {
    "must replace all overrides with one PUT" in {
      wireMockServer.stubFor(
        put(urlEqualTo(path)).willReturn(
          okJson(
            s"""{"zReference":"$testZReference","clock":{"date":"2026-06-17"},"reportingWindow":{"startDate":"2026-06-06T00:00:00Z","endDate":"2026-06-19T23:59:59.999999999Z"}}"""
          )
        )
      )
      val overrides = TestOnlyReportingOverrides(
        LocalDate.parse("2026-06-06"),
        LocalDate.parse("2026-06-19"),
        Some(LocalDate.parse("2026-06-17"))
      )

      val app = application
      running(app) {
        app.injector
          .instanceOf[TestOnlyReportingOverridesConnector]
          .set(testZReference, overrides)(HeaderCarrier())
          .futureValue

        wireMockServer.verify(
          1,
          putRequestedFor(urlEqualTo(path)).withRequestBody(
            equalToJson(
              """{"clock":{"date":"2026-06-17"},"reportingWindow":{"startDate":"2026-06-06T00:00:00Z","endDate":"2026-06-19T23:59:59.999999999Z"}}"""
            )
          )
        )
      }
    }

    "must send a null clock when the optional system date is empty" in {
      wireMockServer.stubFor(
        put(urlEqualTo(path)).willReturn(
          okJson(
            s"""{"zReference":"$testZReference","clock":null,"reportingWindow":{"startDate":"2026-06-06T00:00:00Z","endDate":"2026-06-19T23:59:59.999999999Z"}}"""
          )
        )
      )
      val overrides = TestOnlyReportingOverrides(
        LocalDate.parse("2026-06-06"),
        LocalDate.parse("2026-06-19"),
        None
      )

      val app = application
      running(app) {
        app.injector
          .instanceOf[TestOnlyReportingOverridesConnector]
          .set(testZReference, overrides)(HeaderCarrier())
          .futureValue

        wireMockServer.verify(
          1,
          putRequestedFor(urlEqualTo(path)).withRequestBody(
            equalToJson(
              """{"clock":null,"reportingWindow":{"startDate":"2026-06-06T00:00:00Z","endDate":"2026-06-19T23:59:59.999999999Z"}}"""
            )
          )
        )
      }
    }
  }

  "TestOnlyReportingOverridesConnector.reset" - {
    "must reset all overrides with one DELETE" in {
      wireMockServer.stubFor(
        delete(urlEqualTo(path)).willReturn(
          okJson(s"""{"zReference":"$testZReference","clock":null,"reportingWindow":null}""")
        )
      )

      val app = application
      running(app) {
        app.injector
          .instanceOf[TestOnlyReportingOverridesConnector]
          .reset(testZReference)(HeaderCarrier())
          .futureValue

        wireMockServer.verify(1, deleteRequestedFor(urlEqualTo(path)))
      }
    }
  }
}
