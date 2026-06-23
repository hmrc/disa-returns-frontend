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
import controllers.routes
import models.UserDetails
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(Enrolment(enrolmentKey)).retrieve(
      authorisedEnrolments and credentials and credentialRole and affinityGroup and agentInformation and groupIdentifier
    ) { case enrolments ~ maybeCredentials ~ maybeCredentialRole ~ maybeAffinityGroup ~ agentInfo ~ maybeGroupId =>
      enrolments
        .getEnrolment(enrolmentKey)
        .flatMap(_.getIdentifier(identifierKey))
        .map(_.value)
        .fold {
          logger.warn(s"User with enrolment was missing $identifierKey identifier")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
        } { zReference =>
          (maybeCredentials, maybeAffinityGroup, maybeGroupId) match {
            case (_, Some(Agent), Some(groupId))             =>
              block(
                IdentifierRequest(
                  request,
                  zReference,
                  agentDetails(groupId, agentInfo)
                )
              )
            case (Some(credentials), Some(_), Some(groupId)) =>
              maybeCredentialRole.fold {
                logger.warn("User with DISA enrolment was missing credential role")
                Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
              } { role =>
                block(
                  IdentifierRequest(
                    request,
                    zReference,
                    UserDetails.IsaManager(
                      groupId = groupId,
                      credId = credentials.providerId,
                      credentialRole = role.toString
                    )
                  )
                )
              }
            case _                                           =>
              logger.warn("User with DISA enrolment was missing audit identity details")
              Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
          }
        }
    } recover {
      case _: NoActiveSession        =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }

  private val enrolmentKey  = "HMRC-DISA-ORG"
  private val identifierKey = "ZREF"

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
