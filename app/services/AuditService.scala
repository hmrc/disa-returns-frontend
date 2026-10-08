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
import connectors.DisaAccountConnector
import models.UserDetails
import models.requests.{DataRequest, OptionalDataRequest}
import models.MonthlyReturn
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.DateHelper

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AuditService @Inject() (
  connector: AuditConnector,
  appConfig: FrontendAppConfig,
  dateHelper: DateHelper,
  disaAccountConnector: DisaAccountConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def auditFileUploadStarted[A](
    request: OptionalDataRequest[A],
    monthlyReturn: MonthlyReturn
  )(implicit hc: HeaderCarrier): Future[Unit] =
    retrieveGroupName(request.zReference, AuditTypes.FileUploadStarted).flatMap { groupName =>
      val detail =
        baseDetail(request.zReference, request.userDetails, groupName, request.currentDate, monthlyReturn) ++
          userDetail(request.userDetails)

      sendEvent(AuditTypes.FileUploadStarted, detail)
    }

  def auditFileUploadDeclarationSubmitted[A](
    request: DataRequest[A],
    failureReason: Option[String]
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val monthlyReturn = request.monthlyReturn
    val returnDetail  =
      if (monthlyReturn.nilReturn) {
        Json.obj(EventData.nilReturn -> "yes")
      } else {
        val successfulUploads = monthlyReturn.successfulFileUploads
        val numberOfEntries   =
          successfulUploads.flatMap(_.fileUploadDetails.flatMap(_.validation)).map(_.rowsValidated).sum
        Json.obj(
          EventData.numberOfFiles   -> successfulUploads.size.toString,
          EventData.numberOfEntries -> numberOfEntries.toString
        )
      }
    val statusDetail  = failureReason.fold(Json.obj(EventData.submissionStatus -> SubmissionStatus.Success)) { reason =>
      Json.obj(
        EventData.submissionStatus -> SubmissionStatus.Failure,
        EventData.failureReason    -> reason
      )
    }

    retrieveGroupName(request.zReference, AuditTypes.FileUploadDeclarationSubmitted).flatMap { groupName =>
      val detail =
        baseDetail(request.zReference, request.userDetails, groupName, request.currentDate, monthlyReturn) ++
          userDetail(request.userDetails) ++
          returnDetail ++
          statusDetail

      sendEvent(AuditTypes.FileUploadDeclarationSubmitted, detail)
    }
  }

  private def sendEvent(auditType: String, detail: JsObject)(implicit hc: HeaderCarrier): Future[Unit] = {
    val event = ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType = auditType,
      tags = getAuditTags,
      detail = detail
    )

    connector
      .sendExtendedEvent(event)
      .map(logResponse(_, auditType))
  }

  private def retrieveGroupName(zReference: String, auditType: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] =
    disaAccountConnector
      .getCompanyName(zReference)
      .map { companyName =>
        if (companyName.isEmpty) {
          logger.warn(s"$auditType audit sent without groupName, no company name found for zRef: [$zReference]")
        }
        companyName
      }
      .recover { case NonFatal(e) =>
        logger.warn(
          s"$auditType audit sent without groupName, failed to retrieve company name for zRef: [$zReference]",
          e
        )
        None
      }

  private def baseDetail(
    zReference: String,
    userDetails: UserDetails,
    groupName: Option[String],
    currentDate: LocalDate,
    monthlyReturn: MonthlyReturn
  ): JsObject =
    Json.obj(
      EventData.internalReturnId -> monthlyReturn.submissionId.toString,
      EventData.period           -> dateHelper.reportingPeriod(currentDate),
      EventData.groupId          -> userDetails.groupId
    ) ++
      groupName.fold(Json.obj())(name => Json.obj(EventData.groupName -> name)) ++
      Json.obj(
        EventData.zReference -> zReference,
        EventData.userType   -> userDetails.userType
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
  val FileUploadStarted              = "FileUploadStarted"
  val FileUploadDeclarationSubmitted = "FileUploadDeclarationSubmitted"
}

object SubmissionStatus {
  val Success = "Success"
  val Failure = "Failure"
}

object EventData {
  val internalReturnId = "internalReturnId"
  val period           = "period"
  val groupId          = "groupId"
  val groupName        = "groupName"
  val zReference       = "zReference"
  val userType         = "userType"
  val credId           = "credId"
  val credentialRole   = "credentialRole"
  val agentId          = "agentId"
  val agentName        = "agentName"
  val nilReturn        = "nilReturn"
  val numberOfFiles    = "numberOfFiles"
  val numberOfEntries  = "numberOfEntries"
  val submissionStatus = "submissionStatus"
  val failureReason    = "failureReason"
}
