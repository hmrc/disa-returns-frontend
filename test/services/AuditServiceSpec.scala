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

import base.SpecBase
import config.FrontendAppConfig
import connectors.DisaAccountConnector
import models.{FileUpload, FileUploadDetails, FileUploadStatus, UserDetails, ValidationResult}
import models.requests.{DataRequest, OptionalDataRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.DateHelper

import java.time.LocalDate

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase with MockitoSugar {

  private trait TestSetup {
    val auditConnector: AuditConnector = mock[AuditConnector]
    val appConfig: FrontendAppConfig   = mock[FrontendAppConfig]
    val dateHelper                     = new DateHelper(testReportingWindowClock)
    val disaAccountConnector           = mock[DisaAccountConnector]
    val service                        = new AuditService(auditConnector, appConfig, dateHelper, disaAccountConnector)

    when(appConfig.appName).thenReturn(testAppName)
    when(disaAccountConnector.getCompanyName(eqTo(testZReference))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(testCompanyName)))
    when(auditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any, any))
      .thenReturn(Future.successful(Success))

    def captureEvent(): ExtendedDataEvent = {
      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(captor.capture())(any, any)
      captor.getValue
    }
  }

  "AuditService.auditFileUploadStarted" - {

    "must send FileUploadStarted with IM detail fields" in new TestSetup {
      val request = OptionalDataRequest(
        FakeRequest(),
        testZReference,
        UserDetails.IsaManager(
          groupId = testGroupId,
          credId = testIsaManagerCredId,
          credentialRole = testCredentialRole
        ),
        monthlyReturn = None,
        currentDate = LocalDate.now(testReportingWindowClock),
        reportingWindowOpen = true
      )

      service.auditFileUploadStarted(request, emptyMonthlyReturn).futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      event.auditSource mustEqual testAppName
      event.auditType mustEqual AuditTypes.FileUploadStarted
      (detail \ EventData.internalReturnId).as[String] mustEqual emptyMonthlyReturn.submissionId.toString
      (detail \ EventData.period).as[String] mustEqual testReportingPeriod
      (detail \ EventData.groupId).as[String] mustEqual testGroupId
      (detail \ EventData.groupName).as[String] mustEqual testCompanyName
      detail.keys.toSeq.take(5) mustEqual Seq(
        EventData.internalReturnId,
        EventData.period,
        EventData.groupId,
        EventData.groupName,
        EventData.zReference
      )
      (detail \ EventData.zReference).as[String] mustEqual testZReference
      (detail \ EventData.userType).as[String] mustEqual testIsaManagerUserType
      (detail \ EventData.credId).as[String] mustEqual testIsaManagerCredId
      (detail \ EventData.credentialRole).as[String] mustEqual testCredentialRole
      (detail \ EventData.agentId).toOption mustEqual None
      (detail \ EventData.agentName).toOption mustEqual None
    }

    "must send FileUploadStarted with Agent detail fields" in new TestSetup {
      val request = OptionalDataRequest(
        FakeRequest(),
        testZReference,
        UserDetails.Agent(
          groupId = testGroupId,
          agentId = testAgentId,
          agentName = testAgentName
        ),
        monthlyReturn = None,
        currentDate = LocalDate.now(testReportingWindowClock),
        reportingWindowOpen = true
      )

      service.auditFileUploadStarted(request, emptyMonthlyReturn).futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      event.auditSource mustEqual testAppName
      event.auditType mustEqual AuditTypes.FileUploadStarted
      (detail \ EventData.internalReturnId).as[String] mustEqual emptyMonthlyReturn.submissionId.toString
      (detail \ EventData.period).as[String] mustEqual testReportingPeriod
      (detail \ EventData.groupId).as[String] mustEqual testGroupId
      (detail \ EventData.groupName).as[String] mustEqual testCompanyName
      (detail \ EventData.zReference).as[String] mustEqual testZReference
      (detail \ EventData.userType).as[String] mustEqual testAgentUserType
      (detail \ EventData.agentId).as[String] mustEqual testAgentId
      (detail \ EventData.agentName).as[String] mustEqual testAgentName
      (detail \ EventData.credId).toOption mustEqual None
      (detail \ EventData.credentialRole).toOption mustEqual None
    }
  }

  "AuditService.auditFileUploadDeclarationSubmitted" - {

    def validatedUpload(reference: String, status: String, rows: Int): FileUpload =
      FileUpload(
        reference = reference,
        status = status,
        fileUploadDetails = Some(
          FileUploadDetails(s"$reference.csv", Some(ValidationResult(rows, 0, FileUploadStatus.ValidationSuccess)))
        )
      )

    "must send a Success event with file counts for an Agent submitting a non-nil return" in new TestSetup {
      val monthlyReturn = emptyMonthlyReturn.copy(
        nilReturn = false,
        fileUploads = Seq(
          validatedUpload("first", FileUploadStatus.ValidationSuccess, 100),
          validatedUpload("second", FileUploadStatus.ValidationSuccess, 109),
          validatedUpload("rejected", FileUploadStatus.ValidationFailure, 50)
        )
      )
      val request       = DataRequest(
        FakeRequest(),
        testZReference,
        UserDetails.Agent(
          groupId = testGroupId,
          agentId = testAgentId,
          agentName = testAgentName
        ),
        monthlyReturn = monthlyReturn,
        currentDate = LocalDate.now(testReportingWindowClock),
        reportingWindowOpen = true
      )

      service.auditFileUploadDeclarationSubmitted(request, failureReason = None).futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      event.auditSource mustEqual testAppName
      event.auditType mustEqual AuditTypes.FileUploadDeclarationSubmitted
      (detail \ EventData.internalReturnId).as[String] mustEqual monthlyReturn.submissionId.toString
      (detail \ EventData.period).as[String] mustEqual testReportingPeriod
      (detail \ EventData.groupId).as[String] mustEqual testGroupId
      (detail \ EventData.groupName).as[String] mustEqual testCompanyName
      (detail \ EventData.zReference).as[String] mustEqual testZReference
      (detail \ EventData.userType).as[String] mustEqual testAgentUserType
      (detail \ EventData.agentId).as[String] mustEqual testAgentId
      (detail \ EventData.agentName).as[String] mustEqual testAgentName
      (detail \ EventData.nilReturn).toOption mustEqual None
      (detail \ EventData.numberOfFiles).as[String] mustEqual "2"
      (detail \ EventData.numberOfEntries).as[String] mustEqual "209"
      (detail \ EventData.submissionStatus).as[String] mustEqual SubmissionStatus.Success
      (detail \ EventData.failureReason).toOption mustEqual None
    }

    "must send a Failure event with a reason and no file counts for an IM submitting a nil return" in new TestSetup {
      val monthlyReturn = emptyMonthlyReturn.copy(nilReturn = true)
      val request       = DataRequest(
        FakeRequest(),
        testZReference,
        UserDetails.IsaManager(
          groupId = testGroupId,
          credId = testIsaManagerCredId,
          credentialRole = testCredentialRole
        ),
        monthlyReturn = monthlyReturn,
        currentDate = LocalDate.now(testReportingWindowClock),
        reportingWindowOpen = true
      )

      service
        .auditFileUploadDeclarationSubmitted(request, failureReason = Some("description of failure"))
        .futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      event.auditType mustEqual AuditTypes.FileUploadDeclarationSubmitted
      (detail \ EventData.groupName).as[String] mustEqual testCompanyName
      (detail \ EventData.userType).as[String] mustEqual testIsaManagerUserType
      (detail \ EventData.credId).as[String] mustEqual testIsaManagerCredId
      (detail \ EventData.credentialRole).as[String] mustEqual testCredentialRole
      (detail \ EventData.nilReturn).as[String] mustEqual "yes"
      (detail \ EventData.numberOfFiles).toOption mustEqual None
      (detail \ EventData.numberOfEntries).toOption mustEqual None
      (detail \ EventData.submissionStatus).as[String] mustEqual SubmissionStatus.Failure
      (detail \ EventData.failureReason).as[String] mustEqual "description of failure"
    }

    Seq(
      "no company name is found"      -> Future.successful(None),
      "the company name lookup fails" -> Future.failed(UpstreamErrorResponse("boom", 500))
    ).foreach { case (description, lookupResult) =>
      s"must still send the event without groupName when $description" in new TestSetup {
        when(disaAccountConnector.getCompanyName(eqTo(testZReference))(any[HeaderCarrier]))
          .thenReturn(lookupResult)

        val request = DataRequest(
          FakeRequest(),
          testZReference,
          UserDetails.IsaManager(
            groupId = testGroupId,
            credId = testIsaManagerCredId,
            credentialRole = testCredentialRole
          ),
          monthlyReturn = emptyMonthlyReturn.copy(nilReturn = true),
          currentDate = LocalDate.now(testReportingWindowClock),
          reportingWindowOpen = true
        )

        service.auditFileUploadDeclarationSubmitted(request, failureReason = None).futureValue mustEqual ()

        val detail = captureEvent().detail.as[JsObject]

        (detail \ EventData.groupId).as[String] mustEqual testGroupId
        (detail \ EventData.groupName).toOption mustEqual None
      }
    }
  }
}
