/*
 * Copyright 2025 HM Revenue & Customs
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

import base.SpecBase
import config.FrontendAppConfig
import connectors.DisaAccountConnector
import controllers.routes
import models.{ReportingContext, UserDetails}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ReportingContextSource
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, EmptyRetrieval, Retrieval, ~}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.Constants.delegatedAuthRule

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  private val initialPredicate  =
    (AffinityGroup.Agent and Enrolment(testAgentEnrolmentKey)) or
      (AffinityGroup.Organisation and Enrolment(testDisaEnrolmentKey))
  private val initialRetrieval  =
    authorisedEnrolments and credentials and credentialRole and affinityGroup and agentInformation and groupIdentifier
  private val managerEnrolments = Enrolments(
    Set(
      Enrolment(
        testDisaEnrolmentKey,
        Seq(EnrolmentIdentifier(testZReferenceIdentifierKey, testZReference)),
        "Activated"
      )
    )
  )
  private val agentEnrolments   = Enrolments(Set(Enrolment(testAgentEnrolmentKey)))
  private val reportingContext  = ReportingContext(LocalDate.parse("2026-06-17"), reportingWindowOpen = false)

  private class TestAuthConnector(
    group: Option[AffinityGroup] = Some(AffinityGroup.Organisation),
    enrolments: Enrolments = managerEnrolments,
    creds: Option[uk.gov.hmrc.auth.core.retrieve.Credentials] = Some(
      uk.gov.hmrc.auth.core.retrieve.Credentials(testCredId, testProviderType)
    ),
    role: Option[CredentialRole] = Some(User),
    groupId: Option[String] = Some(testGroupId),
    agentInfo: AgentInformation = AgentInformation(Some(testAgentId), None, Some(testAgentName)),
    initialFailure: Option[Throwable] = None,
    delegatedFailure: Option[Throwable] = None
  ) extends AuthConnector {
    var predicates: Vector[Predicate] = Vector.empty

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] = {
      predicates = predicates :+ predicate
      if (predicate == initialPredicate && retrieval == initialRetrieval) {
        initialFailure.fold(
          Future.successful(
            new ~(new ~(new ~(new ~(new ~(enrolments, creds), role), group), agentInfo), groupId).asInstanceOf[A]
          )
        )(Future.failed)
      } else if (
        predicate == Enrolment(testDisaEnrolmentKey)
          .withIdentifier(testZReferenceIdentifierKey, testZReference)
          .withDelegatedAuthRule(delegatedAuthRule) && retrieval == EmptyRetrieval
      ) {
        delegatedFailure.fold(Future.successful(().asInstanceOf[A]))(Future.failed)
      } else {
        Future.failed(new AssertionError(s"Unexpected Auth request: $predicate / $retrieval"))
      }
    }
  }

  private def withAction(
    auth: TestAuthConnector,
    enabled: Boolean = true,
    preferences: Future[Option[String]] = Future.successful(Some(testZReference))
  )(test: (AuthenticatedIdentifierAction, DisaAccountConnector, ReportingContextSource) => Unit): Unit = {
    val application = applicationBuilder().configure("features.agent-authentication-enabled" -> enabled).build()
    running(application) {
      val account = mock[DisaAccountConnector]
      val source  = mock[ReportingContextSource]
      when(account.getAgentZReference()(any[HeaderCarrier])).thenReturn(preferences)
      when(source.get(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(reportingContext))
      val action  = new AuthenticatedIdentifierAction(
        auth,
        application.injector.instanceOf[FrontendAppConfig],
        source,
        application.injector.instanceOf[BodyParsers.Default],
        account
      )
      test(action, account, source)
    }
  }

  private def agent(
    delegatedFailure: Option[Throwable] = None,
    groupId: Option[String] = Some(testGroupId)
  ): TestAuthConnector = new TestAuthConnector(
    group = Some(AffinityGroup.Agent),
    enrolments = agentEnrolments,
    creds = None,
    role = None,
    groupId = groupId,
    delegatedFailure = delegatedFailure
  )

  "ISA manager authentication" - {
    "must preserve the ZReference, audit identity and reporting context without calling agent preferences" in {
      val auth = new TestAuthConnector
      withAction(auth, enabled = false) { (action, account, source) =>
        val result = action.invokeBlock(
          FakeRequest(),
          request => {
            request.zReference mustBe testZReference
            request.userDetails mustBe UserDetails.IsaManager(testGroupId, testCredId, User.toString)
            request.currentDate mustBe reportingContext.effectiveDate
            request.reportingWindowOpen mustBe false
            Future.successful(Results.Ok)
          }
        )
        status(result) mustBe OK
        auth.predicates mustBe Vector(initialPredicate)
        verify(source).get(eqTo(testZReference))(any[HeaderCarrier])
        verifyNoInteractions(account)
      }
    }

    "must use the ISA manager's own ZReference when an Organisation has both principal enrolments" in {
      val auth = new TestAuthConnector(
        enrolments = Enrolments(managerEnrolments.enrolments ++ agentEnrolments.enrolments)
      )
      withAction(auth, preferences = Future.successful(Some(secondTestZReference))) { (action, account, source) =>
        val result = action.invokeBlock(
          FakeRequest(),
          request => {
            request.userDetails mustBe UserDetails.IsaManager(testGroupId, testCredId, User.toString)
            Future.successful(Results.Ok(request.zReference))
          }
        )
        status(result) mustBe OK
        contentAsString(result) mustBe testZReference
        auth.predicates mustBe Vector(initialPredicate)
        verify(source).get(eqTo(testZReference))(any[HeaderCarrier])
        verifyNoInteractions(account)
      }
    }

    "must authenticate without reporting context when requested" in {
      withAction(new TestAuthConnector) { (action, account, source) =>
        val result = action.invokeBlockWithoutReportingContext(
          FakeRequest(),
          zReference => Future.successful(Results.Ok(zReference))
        )
        status(result) mustBe OK
        contentAsString(result) mustBe testZReference
        verifyNoInteractions(account, source)
      }
    }

    Seq(
      "missing ZREF"              -> (() =>
        new TestAuthConnector(enrolments = Enrolments(Set(Enrolment(testDisaEnrolmentKey, Seq.empty, "Activated"))))
      ),
      "missing enrolment"         -> (() => new TestAuthConnector(enrolments = Enrolments(Set.empty))),
      "missing credential role"   -> (() => new TestAuthConnector(role = None)),
      "missing credentials"       -> (() => new TestAuthConnector(creds = None)),
      "missing affinity group"    -> (() => new TestAuthConnector(group = None)),
      "Individual affinity group" -> (() => new TestAuthConnector(group = Some(AffinityGroup.Individual))),
      "missing group ID"          -> (() => new TestAuthConnector(groupId = None))
    ).foreach { case (label, auth) =>
      s"must reject $label" in {
        withAction(auth()) { (action, account, source) =>
          val result = action.invokeBlock(FakeRequest(), _ => fail("Protected block was invoked"))
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
          verifyNoInteractions(account, source)
        }
      }
    }
  }

  "Agent authentication" - {
    "must authorise the selected ZReference without a principal DISA enrolment, credentials or role" in {
      val auth = agent()
      withAction(auth) { (action, account, source) =>
        val result = action.invokeBlock(
          FakeRequest(),
          request => {
            request.zReference mustBe testZReference
            request.userDetails mustBe UserDetails.Agent(testGroupId, testAgentId, testAgentName)
            request.currentDate mustBe reportingContext.effectiveDate
            Future.successful(Results.Ok)
          }
        )
        status(result) mustBe OK
        auth.predicates.size mustBe 2
        verify(account).getAgentZReference()(any[HeaderCarrier])
        verify(source).get(eqTo(testZReference))(any[HeaderCarrier])
      }
    }

    "must use the selected client when an Agent has both principal enrolments and preserve unknown audit fields" in {
      val otherEnrolment = Enrolments(
        agentEnrolments.enrolments +
          Enrolment(
            testDisaEnrolmentKey,
            Seq(EnrolmentIdentifier(testZReferenceIdentifierKey, secondTestZReference)),
            "Activated"
          )
      )
      val auth           = new TestAuthConnector(
        group = Some(AffinityGroup.Agent),
        enrolments = otherEnrolment,
        agentInfo = AgentInformation(None, None, None)
      )
      withAction(auth) { (action, _, _) =>
        val result = action.invokeBlock(
          FakeRequest(),
          request => {
            request.zReference mustBe testZReference
            request.userDetails mustBe UserDetails.Agent(testGroupId, UserDetails.unknown, UserDetails.unknown)
            Future.successful(Results.Ok)
          }
        )
        status(result) mustBe OK
        auth.predicates.size mustBe 2
      }
    }

    Seq(false, true).foreach { withoutContext =>
      s"must check delegated access when withoutReportingContext=$withoutContext" in {
        val auth = agent()
        withAction(auth) { (action, _, source) =>
          val result = if (withoutContext) {
            action.invokeBlockWithoutReportingContext(
              FakeRequest(),
              reference => Future.successful(Results.Ok(reference))
            )
          } else {
            action.invokeBlock(FakeRequest(), request => Future.successful(Results.Ok(request.zReference)))
          }
          status(result) mustBe OK
          contentAsString(result) mustBe testZReference
          auth.predicates.size mustBe 2
          if (withoutContext) verifyNoInteractions(source)
          else verify(source).get(eqTo(testZReference))(any[HeaderCarrier])
        }
      }

      Seq(UNAUTHORIZED, FORBIDDEN).foreach { responseStatus =>
        s"must return preferences status $responseStatus when withoutReportingContext=$withoutContext" in {
          val auth = agent()
          withAction(auth, preferences = Future.failed(UpstreamErrorResponse("preferences rejected", responseStatus))) {
            (action, account, source) =>
              val result = if (withoutContext) {
                action.invokeBlockWithoutReportingContext(FakeRequest(), _ => fail("Protected block was invoked"))
              } else {
                action.invokeBlock(FakeRequest(), _ => fail("Protected block was invoked"))
              }
              status(result) mustBe responseStatus
              redirectLocation(result) mustBe None
              auth.predicates mustBe Vector(initialPredicate)
              verify(account).getAgentZReference()(any[HeaderCarrier])
              verifyNoInteractions(source)
          }
        }
      }

      Seq(
        ("disabled flag", () => agent(), false, Future.successful(Some(testZReference)), false, false),
        (
          "disabled flag with no group ID",
          () => agent(groupId = None),
          false,
          Future.successful(Some(testZReference)),
          false,
          false
        ),
        (
          "disabled flag with a principal enrolment",
          () =>
            new TestAuthConnector(
              group = Some(AffinityGroup.Agent),
              enrolments = Enrolments(agentEnrolments.enrolments ++ managerEnrolments.enrolments)
            ),
          false,
          Future.successful(Some(testZReference)),
          false,
          false
        ),
        ("missing group ID", () => agent(groupId = None), true, Future.successful(Some(testZReference)), false, false),
        ("no preferences", () => agent(), true, Future.successful(None), true, false),
        (
          "missing delegated enrolment",
          () => agent(Some(new InsufficientEnrolments)),
          true,
          Future.successful(Some(testZReference)),
          true,
          false
        ),
        (
          "failed relationship",
          () => agent(Some(new FailedRelationship)),
          true,
          Future.successful(Some(testZReference)),
          true,
          false
        ),
        (
          "expired delegated authentication",
          () => agent(Some(new BearerTokenExpired)),
          true,
          Future.successful(Some(testZReference)),
          true,
          true
        )
      ).foreach { case (label, makeAuth, enabled, preferences, callsAccount, login) =>
        s"must reject $label when withoutReportingContext=$withoutContext" in {
          withAction(makeAuth(), enabled, preferences) { (action, account, source) =>
            val result = if (withoutContext) {
              action.invokeBlockWithoutReportingContext(FakeRequest(), _ => fail("Protected block was invoked"))
            } else {
              action.invokeBlock(FakeRequest(), _ => fail("Protected block was invoked"))
            }
            status(result) mustBe SEE_OTHER
            if (login) redirectLocation(result).value must startWith("http://localhost:9949/auth-login-stub/gg-sign-in")
            else redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
            verifyNoInteractions(source)
            if (callsAccount) verify(account).getAgentZReference()(any[HeaderCarrier])
            else verifyNoInteractions(account)
          }
        }
      }
    }

    "must propagate preferences service and decoding failures without invoking protected code" in {
      Seq(new RuntimeException("invalid preferences"), UpstreamErrorResponse("unavailable", SERVICE_UNAVAILABLE))
        .foreach { error =>
          withAction(agent(), preferences = Future.failed(error)) { (action, _, source) =>
            val result = action.invokeBlock(FakeRequest(), _ => fail("Protected block was invoked"))
            result.failed.futureValue mustBe error
            verifyNoInteractions(source)
          }
        }
    }

    Seq(UNAUTHORIZED, FORBIDDEN).foreach { responseStatus =>
      s"must propagate protected block failures with status $responseStatus" in {
        withAction(agent()) { (action, _, _) =>
          val error  = UpstreamErrorResponse("downstream rejected", responseStatus)
          val result = action.invokeBlock(FakeRequest(), _ => Future.failed(error))
          result.failed.futureValue mustBe error
        }
      }
    }
  }

  "Initial authentication failures" - {
    Seq(
      new MissingBearerToken,
      new BearerTokenExpired,
      new InsufficientEnrolments,
      new InsufficientConfidenceLevel,
      new UnsupportedAuthProvider,
      new UnsupportedAffinityGroup,
      new UnsupportedCredentialRole
    ).foreach { error =>
      s"must handle ${error.getClass.getSimpleName}" in {
        withAction(new TestAuthConnector(initialFailure = Some(error))) { (action, account, source) =>
          val result = action.invokeBlock(FakeRequest(), _ => fail("Protected block was invoked"))
          status(result) mustBe SEE_OTHER
          if (error.isInstanceOf[NoActiveSession]) {
            redirectLocation(result).value must include("continue=")
            redirectLocation(result).value must startWith("http://localhost:9949/auth-login-stub/gg-sign-in")
          } else redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
          verifyNoInteractions(account, source)
        }
      }
    }
  }
}
