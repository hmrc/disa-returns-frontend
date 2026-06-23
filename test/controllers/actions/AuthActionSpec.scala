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

import base.{SpecBase, TestData}
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(request => Results.Ok(request.zReference))

    def userType(): Action[AnyContent] = authAction(request => Results.Ok(request.userDetails.userType))

    def groupId(): Action[AnyContent] = authAction(request => Results.Ok(request.userDetails.groupId))
  }

  "Auth Action" - {

    "when the user has a DISA enrolment with a ZREF identifier" - {

      "must use it as the request identifier" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeSuccessfulZReferenceAuthConnector(testZReference),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe testZReference
        }
      }

      "must capture the auth group id" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeSuccessfulZReferenceAuthConnector(testZReference),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.groupId()(FakeRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe testGroupId
        }
      }
    }

    "when the user is an agent without a credential role" - {

      "must allow the request and capture the agent user type" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeSuccessfulZReferenceAuthConnector(
              testZReference,
              credentialRole = None,
              affinityGroup = AffinityGroup.Agent,
              agentInformation = AgentInformation(
                agentId = Some(testAgentId),
                agentCode = None,
                agentFriendlyName = Some(testAgentName)
              )
            ),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.userType()(FakeRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe testAgentUserType
        }
      }

      "must allow the request when credentials are unavailable" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeSuccessfulZReferenceAuthConnector(
              testZReference,
              credentials = None,
              credentialRole = None,
              affinityGroup = AffinityGroup.Agent
            ),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.userType()(FakeRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe testAgentUserType
        }
      }
    }

    "when an ISA Manager is missing a credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeSuccessfulZReferenceAuthConnector(
              testZReference,
              credentialRole = None,
              affinityGroup = AffinityGroup.Organisation
            ),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user does not have a ZREF identifier" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeMissingZReferenceAuthConnector,
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new MissingBearerToken),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new BearerTokenExpired),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientEnrolments),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.failed(exceptionToReturn)
}

class FakeSuccessfulZReferenceAuthConnector(
  zReference: String,
  credentials: Option[Credentials] = Some(Credentials(TestData.testCredId, TestData.testProviderType)),
  credentialRole: Option[CredentialRole] = Some(User),
  affinityGroup: AffinityGroup = AffinityGroup.Organisation,
  agentInformation: AgentInformation = AgentInformation(None, None, None),
  groupId: Option[String] = Some(TestData.testGroupId)
) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.successful(
      new ~(
        new ~(
          new ~(
            new ~(
              new ~(
                Enrolments(
                  Set(
                    Enrolment(
                      TestData.testDisaEnrolmentKey,
                      Seq(EnrolmentIdentifier(TestData.testZReferenceIdentifierKey, zReference)),
                      TestData.testActivatedEnrolmentStatus
                    )
                  )
                ),
                credentials
              ),
              credentialRole
            ),
            Some(affinityGroup)
          ),
          agentInformation
        ),
        groupId
      ).asInstanceOf[A]
    )
}

class FakeMissingZReferenceAuthConnector extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.successful(
      new ~(
        new ~(
          new ~(
            new ~(
              new ~(
                Enrolments(Set.empty),
                Some(Credentials(TestData.testCredId, TestData.testProviderType))
              ),
              Some(User)
            ),
            Some(AffinityGroup.Organisation)
          ),
          AgentInformation(None, None, None)
        ),
        Some(TestData.testGroupId)
      ).asInstanceOf[A]
    )
}
