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

package testOnly.connectors

import config.FrontendAppConfig
import play.api.libs.json.{JsNull, JsValue, Json, OFormat, OWrites, Reads}
import play.api.libs.ws.writeableOf_JsValue
import testOnly.forms.TestOnlyReportingOverrides
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.time.{Instant, LocalDate, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import TestOnlyReportingOverridesConnector.{ClockOverride, CurrentOverrides, ReportingWindowOverride, TestOverride, TestOverrideRequest}

@Singleton
class TestOnlyReportingOverridesConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) {

  def get(zReference: String)(implicit hc: HeaderCarrier): Future[CurrentOverrides] =
    httpClient.get(url"${overridesUrl(zReference)}").execute[TestOverride].map { response =>
      CurrentOverrides(
        systemDate = response.clock.map(_.date),
        reportingWindow = response.reportingWindow.map { window =>
          LocalDate.ofInstant(window.startDate, ZoneOffset.UTC) ->
            LocalDate.ofInstant(window.endDate, ZoneOffset.UTC)
        }
      )
    }

  def set(zReference: String, overrides: TestOnlyReportingOverrides)(implicit hc: HeaderCarrier): Future[Unit] = {
    val start = overrides.reportingWindowStart.atStartOfDay(ZoneOffset.UTC).toInstant
    val end   = overrides.reportingWindowEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant.minusNanos(1)
    val replacement = TestOverrideRequest(
      clock = overrides.systemDate.map(ClockOverride.apply),
      reportingWindow = Some(ReportingWindowOverride(start, end))
    )

    httpClient
      .put(url"${overridesUrl(zReference)}")
      .withBody(Json.toJson(replacement))
      .execute[TestOverride]
      .map(_ => ())
  }

  def reset(zReference: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.delete(url"${overridesUrl(zReference)}").execute[TestOverride].map(_ => ())

  private def overridesUrl(zReference: String): String =
    s"${appConfig.disaReturnsBackendBaseUrl}/disa-returns-backend/test-only/overrides/$zReference"

}

object TestOnlyReportingOverridesConnector {
  private final case class ClockOverride(date: LocalDate)
  private object ClockOverride {
    implicit val format: OFormat[ClockOverride] = Json.format[ClockOverride]
  }

  private final case class ReportingWindowOverride(startDate: Instant, endDate: Instant)
  private object ReportingWindowOverride {
    implicit val format: OFormat[ReportingWindowOverride] = Json.format[ReportingWindowOverride]
  }

  private final case class TestOverride(
    zReference: String,
    clock: Option[ClockOverride],
    reportingWindow: Option[ReportingWindowOverride]
  )
  private object TestOverride {
    implicit val reads: Reads[TestOverride] = Json.reads[TestOverride]
  }

  private final case class TestOverrideRequest(
    clock: Option[ClockOverride],
    reportingWindow: Option[ReportingWindowOverride]
  )
  private object TestOverrideRequest {
    implicit val writes: OWrites[TestOverrideRequest] = OWrites { value =>
      Json.obj(
        "clock" -> value.clock.fold[JsValue](JsNull)(Json.toJson(_)),
        "reportingWindow" -> value.reportingWindow.fold[JsValue](JsNull)(Json.toJson(_))
      )
    }
  }

  final case class CurrentOverrides(
    systemDate: Option[LocalDate],
    reportingWindow: Option[(LocalDate, LocalDate)]
  )
}
