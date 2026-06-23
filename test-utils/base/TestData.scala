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

package base

import models.{MonthlyReturn, UserDetails}

import java.time.{Clock, Instant, ZoneOffset}
import java.util.UUID
import scala.util.Random

trait TestData {

  protected val testAppName: String                          = TestData.testAppName
  protected val testZReference: String                       = TestData.randomZReference
  protected val testSubmissionId: UUID                       = TestData.randomSubmissionId
  protected val secondTestSubmissionId: UUID                 = TestData.randomSubmissionId
  protected val testReportingWindowInstant: Instant          = TestData.testReportingWindowInstant
  protected val testReportingWindowClock: Clock              = Clock.fixed(testReportingWindowInstant, ZoneOffset.UTC)
  protected val testJanuaryReportingWindowInstant: Instant   = TestData.testJanuaryReportingWindowInstant
  protected val testAprilReportingWindowInstant: Instant     = TestData.testAprilReportingWindowInstant
  protected val testTaxYear: String                          = TestData.testTaxYear
  protected val nextTestTaxYear: String                      = TestData.nextTestTaxYear
  protected val testMonth: Int                               = TestData.testMonth
  protected val testUpscanMinFileSize: Int                   = TestData.testUpscanMinFileSize
  protected val testUpscanMaxFileSize: Int                   = TestData.testUpscanMaxFileSize
  protected val testReportingWindowMonthName: String         = TestData.testReportingWindowMonthName
  protected val testReportingPeriodMonthName: String         = TestData.testReportingPeriodMonthName
  protected val testReportingPeriod: String                  = TestData.testReportingPeriod
  protected val previousYearReportingPeriodMonthName: String = TestData.previousYearReportingPeriodMonthName
  protected val previousYearReportingPeriod: String          = TestData.previousYearReportingPeriod

  protected val testCredId: String                   = TestData.testCredId
  protected val testProviderType: String             = TestData.testProviderType
  protected val testCredentialRole: String           = TestData.testCredentialRole
  protected val testGroupId: String                  = TestData.testGroupId
  protected val testIsaManagerUserType: String       = TestData.testIsaManagerUserType
  protected val testAgentUserType: String            = TestData.testAgentUserType
  protected val testAgentId: String                  = TestData.testAgentId
  protected val testAgentName: String                = TestData.testAgentName
  protected val testIsaManagerCredId: String         = TestData.testIsaManagerCredId
  protected val testDisaEnrolmentKey: String         = TestData.testDisaEnrolmentKey
  protected val testZReferenceIdentifierKey: String  = TestData.testZReferenceIdentifierKey
  protected val testActivatedEnrolmentStatus: String = TestData.testActivatedEnrolmentStatus
  protected val testUserDetails: UserDetails         =
    UserDetails.IsaManager(testGroupId, testCredId, testCredentialRole)

  protected def emptyMonthlyReturn: MonthlyReturn =
    MonthlyReturn(
      submissionId = testSubmissionId,
      nilReturn = false
    )
}

object TestData {

  val testAppName: String                          = "disa-returns-frontend"
  val testReportingWindowInstant: Instant          = Instant.parse("2026-03-15T12:00:00Z")
  val testJanuaryReportingWindowInstant: Instant   = Instant.parse("2026-01-15T12:00:00Z")
  val testAprilReportingWindowInstant: Instant     = Instant.parse("2026-04-01T00:00:00Z")
  val testTaxYear: String                          = "2025-26"
  val nextTestTaxYear: String                      = "2026-27"
  val testMonth: Int                               = 3
  val testUpscanMinFileSize: Int                   = 1
  val testUpscanMaxFileSize: Int                   = 10485760
  val testReportingWindowMonthName: String         = "March"
  val testReportingPeriodMonthName: String         = "February"
  val testReportingPeriod: String                  = "February 2026"
  val previousYearReportingPeriodMonthName: String = "December"
  val previousYearReportingPeriod: String          = "December 2025"
  val testCredId: String                           = "test-cred-id"
  val testProviderType: String                     = "GovernmentGateway"
  val testCredentialRole: String                   = "User"
  val testGroupId: String                          = "testGroupId-8d181856-2103-4183-bbda-b4d37a400d12"
  val testIsaManagerUserType: String               = "IM"
  val testAgentUserType: String                    = "Agent"
  val testAgentId: String                          = "8d181856-2103-4183-bbda-b4d37a400d1"
  val testAgentName: String                        = "testAgent"
  val testIsaManagerCredId: String                 = "5053588723516092"
  val testDisaEnrolmentKey: String                 = "HMRC-DISA-ORG"
  val testZReferenceIdentifierKey: String          = "ZREF"
  val testActivatedEnrolmentStatus: String         = "Activated"

  def randomZReference: String =
    f"Z${Random.nextInt(10000)}%04d"

  def randomSubmissionId: UUID =
    UUID.randomUUID()
}
