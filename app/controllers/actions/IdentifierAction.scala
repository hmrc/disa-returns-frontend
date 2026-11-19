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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.DisaAccountConnector
import controllers.routes
import models.UserDetails
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.http.Status.{FORBIDDEN, UNAUTHORIZED}
import play.api.mvc.Results.*
import play.api.mvc.*
import services.ReportingContextSource
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ~}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest] {

  def invokeBlockWithoutReportingContext[A](request: Request[A], block: String => Future[Result]): Future[Result] =
    invokeBlock(request, identified => block(identified.zReference))
}

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  reportingContextSource: ReportingContextSource,
  val parser: BodyParsers.Default,
  disaAccountConnector: DisaAccountConnector
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    authenticate(request) { (zReference, userDetails) =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      reportingContextSource.get(zReference).flatMap { context =>
        block(
          IdentifierRequest(
            request,
            zReference,
            userDetails,
            context.effectiveDate,
            context.reportingWindowOpen
          )
        )
      }
    }

  override def invokeBlockWithoutReportingContext[A](
    request: Request[A],
    block: String => Future[Result]
  ): Future[Result] =
    authenticate(request)((zReference, _) => block(zReference))

  private def authenticate[A](
    request: Request[A]
  )(block: (String, UserDetails) => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(Enrolment(enrolmentKey) or Agent).retrieve(
      authorisedEnrolments and credentials and credentialRole and affinityGroup and agentInformation and groupIdentifier
    ) { case enrolments ~ maybeCredentials ~ maybeCredentialRole ~ maybeAffinityGroup ~ agentInfo ~ maybeGroupId =>
      (maybeCredentials, maybeAffinityGroup, maybeGroupId) match {
        case (_, Some(Agent), _) if !config.agentAuthenticationEnabled =>
          rejectDisabledAgentAuthentication()
        case (_, Some(Agent), Some(groupId))                           =>
          authenticateAgent(groupId, agentInfo)(block)
        case (_, Some(Agent), None)                                    =>
          rejectMissingAgentGroupId()
        case (Some(credentials), Some(_), Some(groupId))               =>
          authenticateIsaManager(enrolments, credentials, groupId, maybeCredentialRole)(block)
        case _                                                         =>
          rejectMissingAuditIdentity()
      }
    } recover {
      case _: NoActiveSession        =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }

  private val enrolmentKey      = "HMRC-DISA-ORG"
  private val identifierKey     = "ZREF"
  private val delegatedAuthRule = "disa-auth"

  private def authenticateAgent(
    groupId: String,
    agentInformation: AgentInformation
  )(block: (String, UserDetails) => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    disaAccountConnector
      .getAgentZReference()
      .transformWith {
        case Success(Some(zReference))                                                 =>
          authorised(
            Enrolment(enrolmentKey)
              .withIdentifier(identifierKey, zReference)
              .withDelegatedAuthRule(delegatedAuthRule)
          ) {
            block(zReference, agentDetails(groupId, agentInformation))
          }
        case Success(None)                                                             =>
          logger.warn("Agent has no selected ZReference")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
        case Failure(error: UpstreamErrorResponse) if error.statusCode == UNAUTHORIZED =>
          logger.warn("Agent preferences request returned 401 Unauthorized", error)
          // TODO: Redirect to the appropriate screen once the agent authentication screens are developed.
          Future.successful(Unauthorized)
        case Failure(error: UpstreamErrorResponse) if error.statusCode == FORBIDDEN    =>
          logger.warn("Agent preferences request returned 403 Forbidden", error)
          // TODO: Redirect to the appropriate screen once the agent authentication screens are developed.
          Future.successful(Forbidden)
        case Failure(error)                                                            =>
          Future.failed(error)
      }

  private def rejectDisabledAgentAuthentication(): Future[Result] = {
    logger.warn("Agent authentication is disabled")
    Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
  }

  private def rejectMissingAgentGroupId(): Future[Result] = {
    logger.warn("Agent group ID is missing")
    Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
  }

  private def authenticateIsaManager(
    enrolments: Enrolments,
    credentials: Credentials,
    groupId: String,
    maybeCredentialRole: Option[CredentialRole]
  )(block: (String, UserDetails) => Future[Result]): Future[Result] =
    enrolments
      .getEnrolment(enrolmentKey)
      .flatMap(_.getIdentifier(identifierKey))
      .map(_.value)
      .fold {
        logger.warn(s"User with enrolment was missing $identifierKey identifier")
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
      } { zReference =>
        maybeCredentialRole.fold {
          logger.warn("User with DISA enrolment was missing credential role")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
        } { role =>
          block(
            zReference,
            UserDetails.IsaManager(
              groupId = groupId,
              credId = credentials.providerId,
              credentialRole = role.toString
            )
          )
        }
      }

  private def rejectMissingAuditIdentity(): Future[Result] = {
    logger.warn("User with DISA enrolment was missing audit identity details")
    Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
  }

  private def agentDetails(
    groupId: String,
    agentInformation: AgentInformation
  ): UserDetails.Agent =
    UserDetails.Agent(
      groupId = groupId,
      agentId = agentInformation.agentId.getOrElse(UserDetails.unknown),
      agentName = agentInformation.agentFriendlyName.getOrElse(UserDetails.unknown)
    )
}
