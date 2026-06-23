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

package services

import config.FrontendAppConfig
import models.UserDetails
import models.requests.OptionalDataRequest
import models.MonthlyReturn
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.DateHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (
  connector: AuditConnector,
  appConfig: FrontendAppConfig,
  dateHelper: DateHelper
)(implicit ec: ExecutionContext)
    extends Logging {

  def auditFileUploadStarted[A](
    request: OptionalDataRequest[A],
    monthlyReturn: MonthlyReturn
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val detail = baseDetail(request, monthlyReturn) ++ userDetail(request.userDetails)
    val event  = ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType = AuditTypes.FileUploadStarted,
      tags = getAuditTags,
      detail = detail
    )

    connector
      .sendExtendedEvent(event)
      .map(logResponse(_, AuditTypes.FileUploadStarted))
  }

  private def baseDetail[A](request: OptionalDataRequest[A], monthlyReturn: MonthlyReturn): JsObject =
    Json.obj(
      EventData.internalReturnId -> monthlyReturn.submissionId.toString,
      EventData.period           -> dateHelper.reportingPeriod,
      EventData.groupId          -> request.userDetails.groupId,
      EventData.zReference       -> request.zReference,
      EventData.userType         -> request.userDetails.userType
    )

  private def userDetail(userDetails: UserDetails): JsObject =
    userDetails match {
      case im: UserDetails.IsaManager =>
        Json.obj(
          EventData.credId         -> im.credId,
          EventData.credentialRole -> im.credentialRole
        )
      case agent: UserDetails.Agent   =>
        Json.obj(
          EventData.agentId   -> agent.agentId,
          EventData.agentName -> agent.agentName
        )
    }

  private def getAuditTags(implicit hc: HeaderCarrier): Map[String, String] =
    AuditExtensions
      .auditHeaderCarrier(hc)
      .toAuditTags()

  private def logResponse(result: AuditResult, auditType: String): Unit =
    result match {
      case Success         => logger.info(s"$auditType audit successful")
      case Failure(err, _) => logger.warn(s"$auditType Audit Error, message: $err")
      case Disabled        => logger.warn(s"$auditType failure - auditing disabled")
    }
}

object AuditTypes {
  val FileUploadStarted = "FileUploadStarted"
}

object EventData {
  val internalReturnId = "internalReturnId"
  val period           = "period"
  val groupId          = "groupId"
  val zReference       = "zReference"
  val userType         = "userType"
  val credId           = "credId"
  val credentialRole   = "credentialRole"
  val agentId          = "agentId"
  val agentName        = "agentName"
}
