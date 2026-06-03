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

import base.ISpecBase
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import models.MonthlyReturnSubmission
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class BackendConnectorISpec extends ISpecBase with BeforeAndAfterAll with BeforeAndAfterEach {

  private val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  private val submission     = MonthlyReturnSubmission(
    submissionId = testSubmissionId,
    nilReport = true
  )
  private val submissionJson =
    s"""
      |{
      |  "submissionId": "$testSubmissionId",
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

  private val submissionPath =
    s"/disa-returns-backend/monthly-return-submissions/$testZReference/$testTaxYear/$testSubmissionPeriod"

  "BackendConnector" - {

    "must retrieve a stored monthly return submission" in {
      wireMockServer.stubFor(
        get(urlEqualTo(submissionPath))
          .willReturn(okJson(submissionJson))
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector.retrieve(testZReference, testTaxYear, testSubmissionPeriod)(HeaderCarrier()).futureValue

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

        val result = connector.retrieve(testZReference, testTaxYear, testSubmissionPeriod)(HeaderCarrier()).futureValue

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

        val result = connector
          .upsert(submission, testZReference, testTaxYear, testSubmissionPeriod)(HeaderCarrier())
          .futureValue

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
