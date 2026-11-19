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
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import play.api.libs.json.JsResultException
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, UpstreamErrorResponse}

class DisaAccountConnectorISpec extends ISpecBase {

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.disa-account.protocol" -> "http",
        "microservice.services.disa-account.host"     -> "localhost",
        "microservice.services.disa-account.port"     -> wireMockServer.port()
      )
      .build()

  private def appConnector(app: Application): DisaAccountConnector =
    app.injector.instanceOf[DisaAccountConnector]

  private val registrationPath = s"/disa-account/registration/$testZReference"

  private val preferencesPath = "/disa-account/agents/preferences"

  "DisaAccountConnector.getAgentZReference" - {
    "must retrieve the selected reference using the current credentials" in {
      wireMockServer.stubFor(
        get(urlEqualTo(preferencesPath))
          .withHeader("Authorization", equalTo("Bearer agent-token"))
          .willReturn(okJson(s"""{"zReference":"$testZReference"}"""))
      )

      val app = application
      running(app) {
        val hc = HeaderCarrier(authorization = Some(Authorization("Bearer agent-token")))
        appConnector(app).getAgentZReference()(hc).futureValue mustBe Some(testZReference)
      }
      wireMockServer.verify(1, getRequestedFor(urlEqualTo(preferencesPath)))
    }

    Seq("{}", """{"zReference":null}""", """{"zReference":""}""", """{"zReference":"   "}""")
      .foreach { body =>
        s"must return None for an absent reference in $body" in {
          wireMockServer.stubFor(get(urlEqualTo(preferencesPath)).willReturn(okJson(body)))
          val app = application
          running(app) {
            appConnector(app).getAgentZReference()(HeaderCarrier()).futureValue mustBe None
          }
        }
      }

    "must return None when preferences do not exist or have expired" in {
      wireMockServer.stubFor(get(urlEqualTo(preferencesPath)).willReturn(notFound()))
      val app = application
      running(app) {
        appConnector(app).getAgentZReference()(HeaderCarrier()).futureValue mustBe None
      }
    }

    Seq(400, 401, 403, 500, 503).foreach { status =>
      s"must preserve upstream status $status" in {
        wireMockServer.stubFor(get(urlEqualTo(preferencesPath)).willReturn(aResponse().withStatus(status)))
        val app = application
        running(app) {
          val error = appConnector(app).getAgentZReference()(HeaderCarrier()).failed.futureValue
          error mustBe an[UpstreamErrorResponse]
          error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe status
        }
      }
    }

    "must fail when the reference has an invalid type" in {
      wireMockServer.stubFor(get(urlEqualTo(preferencesPath)).willReturn(okJson("""{"zReference":123}""")))
      val app = application
      running(app) {
        appConnector(app).getAgentZReference()(HeaderCarrier()).failed.futureValue mustBe a[JsResultException]
      }
    }

    "must fail when the response is not JSON" in {
      wireMockServer.stubFor(get(urlEqualTo(preferencesPath)).willReturn(ok("invalid JSON")))
      val app = application
      running(app) {
        appConnector(app).getAgentZReference()(HeaderCarrier()).failed.futureValue mustBe a[Exception]
      }
    }
  }

  "DisaAccountConnector.getCompanyName" - {

    "must return the company name from the registration details" in {
      wireMockServer.stubFor(
        get(urlEqualTo(registrationPath))
          .willReturn(
            okJson(
              s"""
                |{
                |  "groupId": "$testGroupId",
                |  "businessVerification": {
                |    "ctUtr": "1234567890",
                |    "companyName": "$testCompanyName",
                |    "companyNumber": "12345678"
                |  },
                |  "isaProductsChangeUnderReview": false
                |}
                |""".stripMargin
            )
          )
      )

      val app = application
      running(app) {
        appConnector(app).getCompanyName(testZReference)(HeaderCarrier()).futureValue mustBe Some(testCompanyName)
      }

      wireMockServer.verify(1, getRequestedFor(urlEqualTo(registrationPath)))
    }

    "must return None when the registration details have no company name" in {
      wireMockServer.stubFor(
        get(urlEqualTo(registrationPath))
          .willReturn(okJson(s"""{"groupId": "$testGroupId", "isaProductsChangeUnderReview": false}"""))
      )

      val app = application
      running(app) {
        appConnector(app).getCompanyName(testZReference)(HeaderCarrier()).futureValue mustBe None
      }
    }

    "must return None when no registration details are found" in {
      wireMockServer.stubFor(
        get(urlEqualTo(registrationPath))
          .willReturn(notFound())
      )

      val app = application
      running(app) {
        appConnector(app).getCompanyName(testZReference)(HeaderCarrier()).futureValue mustBe None
      }
    }

    "must fail with an UpstreamErrorResponse for any other status" in {
      wireMockServer.stubFor(
        get(urlEqualTo(registrationPath))
          .willReturn(serverError())
      )

      val app = application
      running(app) {
        val error = appConnector(app).getCompanyName(testZReference)(HeaderCarrier()).failed.futureValue

        error mustBe an[UpstreamErrorResponse]
        error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
      }
    }
  }
}
