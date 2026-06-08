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
import models.MonthlyReturn
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class BackendConnectorISpec extends ISpecBase with BeforeAndAfterAll with BeforeAndAfterEach {

  private val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  private val monthlyReturn      = MonthlyReturn(
    submissionId = testSubmissionId,
    nilReturn = true
  )
  private val monthlyReturnJson  =
    s"""
      |{
      |  "zReference": "$testZReference",
      |  "submissionId": "$testSubmissionId",
      |  "taxYear": "$testTaxYear",
      |  "month": $testMonth,
      |  "nilReturn": true,
      |  "fileUploads": [],
      |  "createdOn": "2026-03-15T12:00:00Z",
      |  "lastUpdated": "2026-03-15T12:00:00Z"
      |}
      |""".stripMargin
  private val createRequestJson  =
    """
      |{
      |  "nilReturn": true
      |}
      |""".stripMargin
  private val createResponseJson =
    s"""
      |{
      |  "submissionId": "$testSubmissionId"
      |}
      |""".stripMargin
  private val updateRequestJson  =
    """
      |{
      |  "value": true
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

  private val monthlyReturnPath   =
    s"/disa-returns-backend/monthly/$testZReference/$testTaxYear/$testMonth"
  private val updateNilReturnPath = s"$monthlyReturnPath/nilReturn"

  "BackendConnector" - {

    "must retrieve a stored monthly return" in {
      wireMockServer.stubFor(
        get(urlEqualTo(monthlyReturnPath))
          .willReturn(okJson(monthlyReturnJson))
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector.retrieve(testZReference, testTaxYear, testMonth)(HeaderCarrier()).futureValue

        result.value mustEqual monthlyReturn
      }
    }

    "must return None when retrieve returns NotFound" in {
      wireMockServer.stubFor(
        get(urlEqualTo(monthlyReturnPath))
          .willReturn(notFound())
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector.retrieve(testZReference, testTaxYear, testMonth)(HeaderCarrier()).futureValue

        result must not be defined
      }
    }

    "must create a monthly return" in {
      wireMockServer.stubFor(
        post(urlEqualTo(monthlyReturnPath))
          .withRequestBody(equalToJson(createRequestJson))
          .willReturn(created().withHeader("Content-Type", "application/json").withBody(createResponseJson))
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector
          .createMonthlyReturn(true, testZReference, testTaxYear, testMonth)(HeaderCarrier())
          .futureValue

        result mustEqual monthlyReturn
      }

      wireMockServer.verify(
        postRequestedFor(urlEqualTo(monthlyReturnPath))
          .withRequestBody(equalToJson(createRequestJson))
      )
    }

    "must update nilReturn on an existing monthly return" in {
      wireMockServer.stubFor(
        put(urlEqualTo(updateNilReturnPath))
          .withRequestBody(equalToJson(updateRequestJson))
          .willReturn(okJson(monthlyReturnJson))
      )

      val app = application
      running(app) {
        val connector = appConnector(app)

        val result = connector
          .updateNilReturn(true, testZReference, testTaxYear, testMonth)(HeaderCarrier())
          .futureValue

        result mustEqual monthlyReturn
      }

      wireMockServer.verify(
        putRequestedFor(urlEqualTo(updateNilReturnPath))
          .withRequestBody(equalToJson(updateRequestJson))
      )
    }
  }

  private def appConnector(app: Application): BackendConnector =
    app.injector.instanceOf[BackendConnector]
}
