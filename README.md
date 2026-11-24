
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

### Agent authentication

`features.agent-authentication-enabled` controls whether agents can authenticate. It is `true` in the service's
`application.conf` for local development, overridden to `false` by `app-config-base` for deployed environments,
and enabled in QA by its environment override. To disable it locally, run:

```bash
sbt -Dfeatures.agent-authentication-enabled=false run
```

When disabled, agents are redirected to the unauthorised page. ISA manager authentication is unaffected.
Affinity determines the user's role

| Affinity group | Required activated principal enrolment | ZReference used |
| --- | --- | --- |
| Organisation (ISA manager) | `HMRC-DISA-ORG` | The principal enrolment's `ZREF`. |
| Agent | `HMRC-AS-AGENT` | The client selected in the current session's agent preferences. |

Agents also require a group ID. The frontend forwards the user's bearer token to
`GET /disa-account/agents/preferences` to retrieve the session's selected ZReference, then checks delegated access
using `HMRC-DISA-ORG`, identifier `ZREF`, and rule `disa-auth`. Missing preferences redirect to the unauthorised page;
preferences responses of 401 or 403 are returned with the same status while redirect screens are being developed.

Local agent testing requires the agent preferences endpoints from DFI-2914 in `disa-account`. The returns backend
must also support agent authentication and have its own `features.agent-authentication-enabled` flag enabled for
the full returns journey.

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

> **Note**  
> To override the window date then go to http://localhost:1205/submit-manage-isas-return/test-only/reporting-overrides

### Dates and reporting windows

`ReportingContextSource` supplies the effective date and reporting-window status for the ZReference resolved during
authentication. `IdentifierAction` resolves that context once for each authenticated request and carries both values
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
GET http://localhost:1205/submit-manage-isas-return/test-only/reporting-overrides
```

| Method  | Path | Behavior |
|---------| --- | --- |
| `GET`   | `/submit-manage-isas-return/test-only/reporting-overrides` | Load current overrides. |
| `POST`  | `/submit-manage-isas-return/test-only/reporting-overrides` | Validate and set overrides. |
| `POST`  | `/submit-manage-isas-return/test-only/reporting-overrides/reset` | Reset both overrides. |

All three routes use the authentication rules above. Overrides are scoped to the ISA manager's own ZReference or
the agent's selected client ZReference; the page does not accept an arbitrary ZReference.

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
| `features.agent-authentication-enabled` | `true` locally; `false` in app-config-base | Allow agent authentication when enabled; redirect agents to the unauthorised page when disabled. |
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
