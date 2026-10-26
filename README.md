
# disa-registration
Backend microservice for disa-registration-frontend.

## Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/disa-registration/store/:groupId/:taskListJourney` | Updates a task-list section of an active registration journey. |
| `GET` | `/disa-registration/store/:groupId` | Retrieves the stored registration journey for a group. |
| `PUT` | `/disa-registration/journey/:groupId` | Retrieves an existing registration journey or creates a new one for a group. |
| `POST` | `/disa-registration/:groupId/declare-and-submit` | Declares and submits a completed registration and returns its form bundle ID. |
| `POST` | `/disa-registration/callback/subscriptions/:formBundleId` | Receives a Tax Enrolments subscription callback for a submitted registration. |

Path parameters:

- `groupId`: Group identifier for the registration journey.
- `taskListJourney`: Registration section to update. Supported values are `businessVerification`, `organisationDetails`, `organisationEmail`, `isaProducts`, `certificatesOfAuthority`, `liaisonOfficers`, `signatories`, and `thirdPartyOrganisations`.
- `formBundleId`: ETMP form bundle identifier returned when a registration is submitted.

The journey retrieval, creation, and update endpoints require an authenticated session. Requests with no or invalid
authentication return `401 Unauthorized`.

### Update Registration Journey Section

`POST /disa-registration/store/:groupId/:taskListJourney`

Updates one section of an active registration journey. The request body must match the model associated with the
`taskListJourney` path parameter.

Example request for `taskListJourney` set to `organisationDetails`:

```json
{
  "registeredToManageIsa": "no",
  "zRefNumber": "Z1234",
  "fcaNumber": "6743765"
}
```

- Returns `204 No Content` when the section is updated.
- Returns `400 Bad Request` when `taskListJourney` is unsupported or the JSON does not match the selected section.
- Returns `401 Unauthorized` when the request is not authenticated.
- Returns `404 Not Found` when there is no active registration journey for the group.
- Returns `500 Internal Server Error` when an unexpected error prevents the update.

### Retrieve Registration Journey

`GET /disa-registration/store/:groupId`

Retrieves the registration journey stored for the group.

Example response:

```json
{
  "groupId": "0d23f3d9-6635-4d29-8a4b-4f37b9cc9012",
  "enrolmentId": "fc81d7c8-4340-4992-b4a5-51ddd15de882",
  "status": "Active",
  "organisationDetails": {
    "registeredToManageIsa": "no",
    "zRefNumber": "Z1234",
    "fcaNumber": "6743765"
  },
  "lastUpdated": "2026-10-26T10:15:30Z"
}
```

- Returns `200 OK` with the stored journey data.
- Returns `401 Unauthorized` when the request is not authenticated.
- Returns `404 Not Found` when no registration journey exists for the group.
- Returns `500 Internal Server Error` when the journey cannot be retrieved.

### Get or Create Registration Journey

`PUT /disa-registration/journey/:groupId`

Returns the active registration journey for the group, creating one when none exists. No request body is required.

Example response:

```json
{
  "isNewEnrolmentJourney": true,
  "journeyData": {
    "groupId": "0d23f3d9-6635-4d29-8a4b-4f37b9cc9012",
    "enrolmentId": "fc81d7c8-4340-4992-b4a5-51ddd15de882",
    "status": "Active",
    "lastUpdated": "2026-10-26T10:15:30Z"
  }
}
```

- Returns `201 Created` with `isNewEnrolmentJourney` set to `true` when a new journey is created.
- Returns `200 OK` with `isNewEnrolmentJourney` set to `false` when an active journey already exists.
- Returns `401 Unauthorized` when the request is not authenticated.
- Returns `500 Internal Server Error` when the journey cannot be retrieved or created.

### Declare and Submit Registration

`POST /disa-registration/:groupId/declare-and-submit`

Builds and submits an ETMP enrolment from the stored journey, marks the journey as submitted, and requests a Tax
Enrolments subscription. No request body is required.

Example response:

```json
{
  "formBundleId": "123456789012"
}
```

- Returns `200 OK` with the ETMP form bundle ID when the registration is submitted.
- Returns `404 Not Found` when no registration journey exists for the group.
- Returns `500 Internal Server Error` when the journey is incomplete, has no active record to submit, or ETMP submission fails.

### Tax Enrolments Subscription Callback

`POST /disa-registration/callback/subscriptions/:formBundleId`

Receives the outcome of a Tax Enrolments subscription request. Supported `state` values are `SUCCEEDED`, `ERROR`,
`EnrolmentError`, `Enrolled`, and `AuthRefreshed`.

Successful callback request:

```json
{
  "url": "https://example.tax.service.gov.uk/disa-registration/callback/subscriptions/123456789012",
  "state": "SUCCEEDED"
}
```

Error callback request:

```json
{
  "url": "https://example.tax.service.gov.uk/disa-registration/callback/subscriptions/123456789012",
  "state": "ERROR",
  "errorResponse": "Tax Enrolments subscription failed"
}
```

- Returns `204 No Content` for a valid callback payload, including callbacks that report an error state.
- Returns `400 Bad Request` when the request body is missing, is not JSON, or contains an invalid callback payload.

### Before running the app

This repository relies on having mongodb running locally. You can start it with:

```bash
# first check to see if mongo is already running
docker ps | grep mongodb

# if not, start it
docker run --restart unless-stopped --name mongodb -p 27017:27017 -d percona/percona-server-mongodb:7.0 --replSet rs0
```

Reference instructions for [setting up docker](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/install-docker.html) and [running mongodb](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-mongodb.html#install-mongodb-applesilicon-mac).

### Running the app

```bash
sbt run
```

You can then query the app to ensure it is working with the following command:

```bash
# other useful commands
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
### Further documentation

You can view further information regarding this service via our [service guide](#).

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
