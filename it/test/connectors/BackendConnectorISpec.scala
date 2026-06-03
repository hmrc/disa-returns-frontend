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

package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import models.Month.MAR
import models.MonthlyReturnSubmission
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID

class BackendConnectorISpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  private val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  private val submission = MonthlyReturnSubmission(
    submissionId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    nilReport = true
  )
  private val submissionJson =
    """
      |{
      |  "submissionId": "11111111-1111-1111-1111-111111111111",
      |  "nilReport": true
      |}
      |""".stripMargin

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

  private val submissionPath = "/disa-returns-backend/monthly-return-submissions/Z1234/2025-26/MAR"

  "BackendConnector" - {

    "must retrieve a stored monthly return submission" in {
      wireMockServer.stubFor(
        get(urlEqualTo(submissionPath))
          .willReturn(okJson(submissionJson))
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector.retrieve("Z1234", "2025-26", MAR)(HeaderCarrier()).futureValue

        result.value mustEqual submission
      }
    }

    "must return None when retrieve returns NotFound" in {
      wireMockServer.stubFor(
        get(urlEqualTo(submissionPath))
          .willReturn(notFound())
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector.retrieve("Z1234", "2025-26", MAR)(HeaderCarrier()).futureValue

        result must not be defined
      }
    }

    "must upsert a monthly return submission" in {
      wireMockServer.stubFor(
        put(urlEqualTo(submissionPath))
          .withRequestBody(equalToJson(submissionJson))
          .willReturn(created().withHeader("Content-Type", "application/json").withBody(submissionJson))
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector.upsert(submission, "Z1234", "2025-26", MAR)(HeaderCarrier()).futureValue

        result mustEqual submission
      }

      wireMockServer.verify(
        putRequestedFor(urlEqualTo(submissionPath))
          .withRequestBody(equalToJson(submissionJson))
      )
    }
  }

  private def appConnector(app: Application): BackendConnector =
    app.injector.instanceOf[BackendConnector]
}
