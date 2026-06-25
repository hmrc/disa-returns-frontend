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
import models.UserDetails
import models.requests.OptionalDataRequest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.DateHelper

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase with MockitoSugar {

  private trait TestSetup {
    val auditConnector: AuditConnector = mock[AuditConnector]
    val appConfig: FrontendAppConfig   = mock[FrontendAppConfig]
    val dateHelper                     = new DateHelper(testReportingWindowClock)
    val service                        = new AuditService(auditConnector, appConfig, dateHelper)

    when(appConfig.appName).thenReturn(testAppName)
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
        monthlyReturn = None
      )

      service.auditFileUploadStarted(request, emptyMonthlyReturn).futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      event.auditSource mustEqual testAppName
      event.auditType mustEqual AuditTypes.FileUploadStarted
      (detail \ EventData.internalReturnId).as[String] mustEqual emptyMonthlyReturn.submissionId.toString
      (detail \ EventData.period).as[String] mustEqual testReportingPeriod
      (detail \ EventData.groupId).as[String] mustEqual testGroupId
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
        monthlyReturn = None
      )

      service.auditFileUploadStarted(request, emptyMonthlyReturn).futureValue mustEqual ()

      val event  = captureEvent()
      val detail = event.detail.as[JsObject]

      event.auditSource mustEqual testAppName
      event.auditType mustEqual AuditTypes.FileUploadStarted
      (detail \ EventData.internalReturnId).as[String] mustEqual emptyMonthlyReturn.submissionId.toString
      (detail \ EventData.period).as[String] mustEqual testReportingPeriod
      (detail \ EventData.groupId).as[String] mustEqual testGroupId
      (detail \ EventData.zReference).as[String] mustEqual testZReference
      (detail \ EventData.userType).as[String] mustEqual testAgentUserType
      (detail \ EventData.agentId).as[String] mustEqual testAgentId
      (detail \ EventData.agentName).as[String] mustEqual testAgentName
      (detail \ EventData.credId).toOption mustEqual None
      (detail \ EventData.credentialRole).toOption mustEqual None
    }
  }
}
