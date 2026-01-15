# pcq-consolidation-service

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service)

This is the consolidation service for the protected characteristics questionnaire (PCQ) platform. It aggregates and consolidates PCQ responses so the service can report on fairness and equality commitments under the Equality Act 2010.

## What this service does

- Consolidates PCQ responses for downstream reporting and analytics.
- Runs as a Spring Boot API on port `4555` with a standard `/health` endpoint.
- Builds and runs via Gradle and Docker for local development and deployment.

## Overview

<p align="center">
<a href="https://github.com/hmcts/pcq-frontend">pcq-frontend</a> • <a href="https://github.com/hmcts/pcq-backend">pcq-backend</a> • <b><a href="https://github.com/hmcts/pcq-consolidation-service">pcq-consolidation-service</a></b> • <a href="https://github.com/hmcts/pcq-shared-infrastructure">pcq-shared-infrastructure</a> • <a href="https://github.com/hmcts/pcq-loader">pcq-loader</a>
</p>

<br>

<p align="center">
  <img src="https://raw.githubusercontent.com/hmcts/pcq-frontend/master/pcq_overview.png" width="500"/>
</p>

## Notes

Since Spring Boot 2.1, bean overriding is disabled. If you want to enable it, set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is enabled by default in the project. Please refrain from using JUnit 4 and use JUnit 5.

## Environment variables

Runtime configuration is provided via Helm values under `charts/pcq-consolidation-service/values.yaml`, with environment-specific templates in:

- `charts/pcq-consolidation-service/values.preview.template.yaml`
- `charts/pcq-consolidation-service/values.aat.template.yaml`

The main environment variables are defined under `job.environment` (for example, `PCQ_BACKEND_URL`, `S2S_URL`, `IDAM_API_URL`, `CORE_CASE_DATA_API_URL`). Secrets are sourced from Azure Key Vault via `job.keyVaults.pcq.secrets` and mapped to env vars such as `JWT_SECRET`, `IDAM_CLIENT_SECRET`, and `S2S_SECRET`.

## Deployment

This service is deployed as a Kubernetes `CronJob` (see `job.kind` in the Helm values). The schedule is configured in `job.schedule`, and the container image is set in `job.image`. In preview/aat templates, the pipeline injects `${IMAGE_NAME}` and `${SERVICE_FQDN}` at deploy time.

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create the Docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/pcq-consolidation-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `4555` in this consolidation-service app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4555/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

The script includes the minimum environment variables necessary to start the API instance. Whenever any variable is changed or any other script regarding Docker image/container build is updated, the suggested way to ensure all is cleaned up properly is this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. You might also consider removing cluttered images, especially ones you were experimenting with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
