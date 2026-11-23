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

package controllers.actions

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{status => _, *}
import config.FrontendAppConfig
import controllers.routes
import models.{ReportingContext, UserDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verifyNoInteractions, when}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ReportingContextSource
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.Future

class AuthActionISpec extends ISpecBase {
  private val preferencesPath = "/disa-account/agents/preferences"
  private val bearer          = "Bearer agent-token"
  private val initialRequest  = """{
    "authorise":[{"$or":[
      [{"affinityGroup":"Agent"},{"enrolment":"HMRC-AS-AGENT","identifiers":[],"state":"Activated"}],
      [{"affinityGroup":"Organisation"},{"enrolment":"HMRC-DISA-ORG","identifiers":[],"state":"Activated"}]
    ]}],
    "retrieve":["authorisedEnrolments","optionalCredentials","credentialRole","affinityGroup","agentInformation","groupIdentifier"]
  }"""
  private val agentIdentity   = Json.obj(
    "authorisedEnrolments" -> Json.arr(
      Json.obj("key" -> testAgentEnrolmentKey, "identifiers" -> Json.arr(), "state" -> testActivatedEnrolmentStatus)
    ),
    "affinityGroup"        -> "Agent",
    "agentInformation"     -> Json.obj("agentId" -> testAgentId, "agentFriendlyName" -> testAgentName),
    "groupIdentifier"      -> testGroupId
  )

  private def delegatedRequest(reference: String): String = Json.stringify(
    Json.obj(
      "authorise" -> Json.arr(
        Json.obj(
          "enrolment"         -> "HMRC-DISA-ORG",
          "identifiers"       -> Json.arr(Json.obj("key" -> "ZREF", "value" -> reference)),
          "state"             -> "Activated",
          "delegatedAuthRule" -> "disa-auth"
        )
      ),
      "retrieve"  -> Json.arr()
    )
  )

  private def stubIdentity(token: String = bearer, identity: JsObject = agentIdentity): Unit =
    wireMockServer.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withHeader("Authorization", equalTo(token))
        .withRequestBody(equalToJson(initialRequest))
        .willReturn(okJson(Json.stringify(identity)))
    )

  private def stubPreferences(reference: String = testZReference, token: String = bearer): Unit =
    wireMockServer.stubFor(
      get(urlEqualTo(preferencesPath))
        .withHeader("Authorization", equalTo(token))
        .willReturn(okJson(Json.stringify(Json.obj("zReference" -> reference))))
    )

  private def stubDelegated(
    reference: String = testZReference,
    token: String = bearer,
    statusCode: Int = OK
  ): Unit =
    wireMockServer.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withHeader("Authorization", equalTo(token))
        .withRequestBody(equalToJson(delegatedRequest(reference)))
        .willReturn(okJson("{}").withStatus(statusCode))
    )

  private def withAction(enabled: Boolean = true)(
    test: (AuthenticatedIdentifierAction, ReportingContextSource, FrontendAppConfig) => Unit
  ): Unit = {
    val source = mock[ReportingContextSource]
    when(source.get(any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(ReportingContext(LocalDate.parse("2026-06-17"), reportingWindowOpen = true)))
    val app    = new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.protocol"         -> "http",
        "microservice.services.auth.host"             -> "localhost",
        "microservice.services.auth.port"             -> wireMockServer.port(),
        "microservice.services.disa-account.protocol" -> "http",
        "microservice.services.disa-account.host"     -> "localhost",
        "microservice.services.disa-account.port"     -> wireMockServer.port(),
        "features.agent-authentication-enabled"       -> enabled
      )
      .overrides(bind[ReportingContextSource].toInstance(source))
      .build()
    running(app) {
      test(app.injector.instanceOf[AuthenticatedIdentifierAction], source, app.injector.instanceOf[FrontendAppConfig])
    }
  }

  "Authentication over HTTP" - {
    "must allow an Organisation with a principal DISA enrolment when agent authentication is disabled" in {
      stubIdentity(identity =
        Json.obj(
          "authorisedEnrolments" -> Json.arr(
            Json.obj(
              "key"         -> testDisaEnrolmentKey,
              "identifiers" -> Json.arr(Json.obj("key" -> testZReferenceIdentifierKey, "value" -> testZReference)),
              "state"       -> testActivatedEnrolmentStatus
            )
          ),
          "optionalCredentials"  -> Json.obj("providerId" -> testCredId, "providerType" -> testProviderType),
          "credentialRole"       -> "User",
          "affinityGroup"        -> "Organisation",
          "agentInformation"     -> Json.obj(),
          "groupIdentifier"      -> testGroupId
        )
      )
      withAction(enabled = false) { (action, _, _) =>
        val result = action.invokeBlock(
          FakeRequest().withSession(SessionKeys.authToken -> bearer),
          identified => {
            identified.userDetails mustBe UserDetails.IsaManager(testGroupId, testCredId, "User")
            Future.successful(Results.Ok(identified.zReference))
          }
        )
        status(result) mustBe OK
        contentAsString(result) mustBe testZReference
      }
      wireMockServer.verify(0, getRequestedFor(urlEqualTo(preferencesPath)))
      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
    }

    "must reject failed principal enrolment or affinity checks before retrieving preferences" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/auth/authorise"))
          .withRequestBody(equalToJson(initialRequest))
          .willReturn(aResponse().withStatus(UNAUTHORIZED))
      )
      withAction() { (action, source, _) =>
        val result = action.invokeBlock(
          FakeRequest().withSession(SessionKeys.authToken -> bearer),
          _ => fail("Protected block was invoked")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        verifyNoInteractions(source)
      }
      wireMockServer.verify(0, getRequestedFor(urlEqualTo(preferencesPath)))
      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
    }

    Seq(false, true).foreach { withoutContext =>
      s"must allow delegated access when withoutReportingContext=$withoutContext" in {
        stubIdentity()
        stubPreferences()
        stubDelegated()
        withAction() { (action, source, _) =>
          val request = FakeRequest().withSession(SessionKeys.authToken -> bearer)
          val result  = if (withoutContext) {
            action.invokeBlockWithoutReportingContext(request, reference => Future.successful(Results.Ok(reference)))
          } else {
            action.invokeBlock(
              request,
              identified => {
                identified.userDetails mustBe UserDetails.Agent(testGroupId, testAgentId, testAgentName)
                Future.successful(Results.Ok(identified.zReference))
              }
            )
          }
          status(result) mustBe OK
          contentAsString(result) mustBe testZReference
          if (withoutContext) verifyNoInteractions(source)
        }
        wireMockServer.verify(
          1,
          getRequestedFor(urlEqualTo(preferencesPath))
            .withHeader("Authorization", equalTo(bearer))
        )
        wireMockServer.verify(
          1,
          postRequestedFor(urlEqualTo("/auth/authorise"))
            .withRequestBody(equalToJson(delegatedRequest(testZReference)))
        )
      }
    }

    "must reject agents when disabled without retrieving preferences or checking delegation" in {
      stubIdentity()
      withAction(enabled = false) { (action, source, _) =>
        val result = action.invokeBlock(
          FakeRequest().withSession(SessionKeys.authToken -> bearer),
          _ => fail("Protected block was invoked")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        verifyNoInteractions(source)
      }
      wireMockServer.verify(0, getRequestedFor(urlEqualTo(preferencesPath)))
      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
    }

    "must reject an unsuccessful delegated Auth check" in {
      stubIdentity()
      stubPreferences()
      stubDelegated(statusCode = UNAUTHORIZED)
      withAction() { (action, source, _) =>
        val result = action.invokeBlock(
          FakeRequest().withSession(SessionKeys.authToken -> bearer),
          _ => fail("Protected block was invoked")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        verifyNoInteractions(source)
      }
      wireMockServer.verify(
        1,
        postRequestedFor(urlEqualTo("/auth/authorise"))
          .withRequestBody(equalToJson(delegatedRequest(testZReference)))
      )
    }

    Seq(401, 403, 404).foreach { responseStatus =>
      s"must handle preferences status $responseStatus without checking delegation" in {
        stubIdentity()
        wireMockServer.stubFor(
          get(urlEqualTo(preferencesPath))
            .willReturn(aResponse().withStatus(responseStatus))
        )
        withAction() { (action, source, _) =>
          val result = action.invokeBlock(
            FakeRequest().withSession(SessionKeys.authToken -> bearer),
            _ => fail("Protected block was invoked")
          )
          if (responseStatus == NOT_FOUND) {
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
          } else {
            status(result) mustBe responseStatus
            redirectLocation(result) mustBe None
          }
          verifyNoInteractions(source)
        }
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      }
    }

    "must reject preferences with no selected reference" in {
      stubIdentity()
      wireMockServer.stubFor(get(urlEqualTo(preferencesPath)).willReturn(okJson("{}")))
      withAction() { (action, source, _) =>
        val result = action.invokeBlock(
          FakeRequest().withSession(SessionKeys.authToken -> bearer),
          _ => fail("Protected block was invoked")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        verifyNoInteractions(source)
      }
      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
    }

    "must propagate preferences outages" in {
      stubIdentity()
      wireMockServer.stubFor(get(urlEqualTo(preferencesPath)).willReturn(serviceUnavailable()))
      withAction() { (action, source, _) =>
        val result = action.invokeBlock(
          FakeRequest().withSession(SessionKeys.authToken -> bearer),
          _ => fail("Protected block was invoked")
        )
        val error  = result.failed.futureValue
        error mustBe an[UpstreamErrorResponse]
        error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe SERVICE_UNAVAILABLE
        verifyNoInteractions(source)
      }
    }

    "must retrieve each session's selection independently" in {
      val otherBearer = "Bearer other-agent-token"
      Seq((bearer, testZReference), (otherBearer, secondTestZReference)).foreach { case (token, reference) =>
        stubIdentity(token)
        stubPreferences(reference, token)
        stubDelegated(reference, token)
      }
      withAction() { (action, _, _) =>
        Seq((bearer, testZReference), (otherBearer, secondTestZReference), (bearer, testZReference)).foreach {
          case (token, reference) =>
            val result = action.invokeBlockWithoutReportingContext(
              FakeRequest().withSession(SessionKeys.authToken -> token),
              selected => Future.successful(Results.Ok(selected))
            )
            status(result) mustBe OK
            contentAsString(result) mustBe reference
        }
      }
      wireMockServer.verify(
        2,
        getRequestedFor(urlEqualTo(preferencesPath))
          .withHeader("Authorization", equalTo(bearer))
      )
      wireMockServer.verify(
        1,
        getRequestedFor(urlEqualTo(preferencesPath))
          .withHeader("Authorization", equalTo(otherBearer))
      )
    }
  }
}
