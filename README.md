# pcq-consolidation-service

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-consolidation-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-consolidation-service)

This is the frontend for the protected characteristics questionnaire service. This service will ask a set of questions that will help us check we are treating people fairly and equally. It helps us to meet our commitment to equality (under the Equality Act 2010). 

## Overview

<p align="center">
<a href="https://github.com/hmcts/pcq-frontend">pcq-frontend</a> • <a href="https://github.com/hmcts/pcq-backend">pcq-backend</a> • <b><a href="https://github.com/hmcts/pcq-consolidation-service">pcq-consolidation-service</a></b> • <a href="https://github.com/hmcts/pcq-shared-infrastructure">pcq-shared-infrastructure</a> • <a href="https://github.com/hmcts/pcq-loader">pcq-loader</a>
</p>
 
<br>

<p align="center">
  <img src="https://raw.githubusercontent.com/hmcts/pcq-frontend/master/pcq_overview.png" width="500"/>
</p>

## Notes

Since Spring Boot 2.1 bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is now enabled by default in the project. Please refrain from using JUnit4 and use the next generation

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

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/pcq-backend` directory)
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

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
