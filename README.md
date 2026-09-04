
# disa-returns-frontend

Play frontend for submitting monthly DISA returns.

### Before running the app

Monthly-return data and test-only override state are accessed through `disa-returns-backend`. This frontend does not
persist that data itself and does not require a local MongoDB instance.

## Running the app
### Service manager
The whole service can be started with:
```bash
sm2 --start DISA_RETURNS_ALL
```

### Locally

```bash
sbt run
```

The frontend runs on port `1205`. Useful SBT commands include:

```bash
sbt clean

sbt reload

sbt compile
```

### Running the test suite

To run the unit tests:

```bash
sbt test
```

To run the integration tests:

```bash
sbt it/test
```

### Test-only reporting overrides

Start both `disa-returns-submission` and `disa-returns-backend` from their repositories with the test-only router:

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

Then run the same command from this frontend repository.

On the frontend, enabling the test-only router both exposes the test-only page and selects the backend-driven
reporting-context binding.

### Dates and reporting windows

`ReportingContextSource` supplies the effective date and reporting-window status for the authenticated user's enrolled
Z-reference. `IdentifierAction` resolves that context once for each authenticated request and carries both values
through `IdentifierRequest`, `OptionalDataRequest`, and `DataRequest`.

The same request-scoped date is used to derive the previous reporting period, address monthly-return backend routes,
build Upscan callback URLs, render reporting-period content, and populate audit events. Journey guards use the captured
reporting-window boolean and do not perform another source or HTTP call.

With the normal router, `SystemReportingContextSource` gets the effective date from the frontend's local UTC
`SystemClock` and gets authoritative reporting-window status from
`GET /disa-returns-backend/reporting-window/status/:zReference`. With the test-only router,
`TestOnlyBackendReportingContextSource` reads the optional date from one backend aggregate overrides GET, falls back to the
frontend's local `SystemClock` when no clock override exists, and gets reporting-window status through the same normal
backend status endpoint used in production.

### Override page

The authenticated page is available at:

```text
GET http://localhost:1205/obligations/returns/isa/test-only/reporting-overrides
```

| Method | Path | Behavior |
| --- | --- | --- |
| `GET` | `/obligations/returns/isa/test-only/reporting-overrides` | Load current overrides. |
| `POST` | `/obligations/returns/isa/test-only/reporting-overrides` | Validate and set overrides. |
| `POST` | `/obligations/returns/isa/test-only/reporting-overrides/reset` | Reset both overrides. |

All three routes require the `HMRC-DISA-ORG` enrolment and use its `ZREF` identifier. Overrides are scoped to the
authenticated user's enrolled Z-reference; the page does not accept an arbitrary Z-reference.

Reporting-window start and end dates are required, and the start must be on or before the end. A submission replaces
all overrides with one `PUT` to `/disa-returns-backend/test-only/overrides/:zReference`. The reporting window is sent as
an inclusive UTC interval from the start of the first date through the end of the final date. The system date is
optional:

- entering a date sets the Z-reference-specific clock override
- leaving it empty resets the clock override while still setting the reporting-window override
- **Reset overrides** clears both overrides

Reloading the page uses one aggregate `GET` to read active override values from backend, then repopulates the
corresponding inputs. System-date inputs stay empty when no clock override is active. Reset uses one aggregate `DELETE`.

The frontend does not persist override state and does not call `disa-returns-submission` directly. Test-only override
state uses the aggregate overrides endpoint on `disa-returns-backend`; reporting-window status always uses the normal
backend status endpoint.

### Relevant configuration

| Key | Default | Purpose |
| --- | --- | --- |
| `application.router` | unset | Use `testOnlyDoNotUseInAppConf.Routes` to expose test-only routes and select the aggregate-aware reporting-context source. |
| `microservice.services.disa-returns-backend.port` | `1207` | Backend service port. |

### Before you commit

This service leverages scalaFmt to ensure that the code is formatted correctly.

Before you commit, please run the following commands to check that the code is formatted correctly:

```bash
# checks all source and sbt files are correctly formatted
sbt prePrChecks

# if checks fail, you can format with the following commands

# formats all source files
sbt scalafmtAll

# formats all sbt files
sbt scalafmtSbt

# formats just the main source files (excludes test and configuration files)
sbt scalafmt
```
### License

This code is open source software licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.html).
