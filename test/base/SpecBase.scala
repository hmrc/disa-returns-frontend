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

package base

import controllers.actions._
import models.{Month, MonthlyReturnSubmission}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents

import java.time.{Clock, Instant, ZoneOffset}
import java.util.UUID
import scala.util.Random

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with EitherValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  protected val testZReference: String                       = SpecBase.randomZReference
  protected val testProviderId: String                       = SpecBase.randomProviderId
  protected val secondTestProviderId: String                 = SpecBase.randomProviderId
  protected val testSubmissionId: UUID                       = SpecBase.randomSubmissionId
  protected val secondTestSubmissionId: UUID                 = SpecBase.randomSubmissionId
  protected val testReportingWindowInstant                   = Instant.parse("2026-03-15T12:00:00Z")
  protected val testReportingWindowClock                     = Clock.fixed(testReportingWindowInstant, ZoneOffset.UTC)
  protected val testJanuaryReportingWindowInstant            = Instant.parse("2026-01-15T12:00:00Z")
  protected val testAprilReportingWindowInstant              = Instant.parse("2026-04-01T00:00:00Z")
  protected val testTaxYear: String                          = "2025-26"
  protected val nextTestTaxYear: String                      = "2026-27"
  protected val testSubmissionPeriod: Month.Value            = Month.MAR
  protected val testReportingWindowMonthName: String         = "March"
  protected val testReportingPeriodMonthName: String         = "February"
  protected val previousYearReportingPeriodMonthName: String = "December"

  def emptyMonthlyReturnSubmission: MonthlyReturnSubmission =
    MonthlyReturnSubmission(
      submissionId = testSubmissionId,
      nilReport = false
    )

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(
    monthlyReturnSubmission: Option[MonthlyReturnSubmission] = None
  ): GuiceApplicationBuilder = {
    val bodyParsers = stubControllerComponents().parsers

    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(bodyParsers, testZReference, testProviderId)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(monthlyReturnSubmission))
      )
  }
}

object SpecBase {

  def randomZReference: String =
    f"Z${Random.nextInt(10000)}%04d"

  def randomProviderId: String =
    s"provider-${UUID.randomUUID()}"

  def randomSubmissionId: UUID =
    UUID.randomUUID()
}
