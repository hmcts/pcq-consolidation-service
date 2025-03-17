#renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.1

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

# Change to non-root privilege
USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/pcq-consolidation-service.jar /opt/app/

EXPOSE 4555
CMD [ "pcq-consolidation-service.jar" ]
