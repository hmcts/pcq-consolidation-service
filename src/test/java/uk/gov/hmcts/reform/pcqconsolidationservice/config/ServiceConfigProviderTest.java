package uk.gov.hmcts.reform.pcqconsolidationservice.config;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcqconsolidationservice.exception.ServiceNotConfiguredException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigHelper.serviceConfigProvider;


class ServiceConfigProviderTest {

    @Test
    @SuppressWarnings({"PMD.UnnecessaryFullyQualifiedName", "PMD.JUnitAssertionsShouldIncludeMessage"})
    void configShouldReturnTheRightServiceConfigurationWhenPresent() {
        // given
        ServiceConfigItem service1Config =
                ServiceConfigHelper.serviceConfigItem(
                        "service1",
                        Arrays.asList("ctid1", "ctid2"),
                        singletonList(ServiceConfigHelper.createCaseFieldMap("ACTOR_1", "pcqId1")),
                        "scannedDocuments.value.controlNumber", null);

        ServiceConfigItem service2Config =
                ServiceConfigHelper.serviceConfigItem(
                        "service2",
                        Arrays.asList("ctid1", "ctid3"),
                        singletonList(ServiceConfigHelper.createCaseFieldMap("ACTOR_2", "pcqId2")),
                        "scannedDocuments.value.fileName", ".pdf");

        List<ServiceConfigItem> configuredServices = Arrays.asList(service1Config, service2Config);

        // when
        ServiceConfigProvider serviceConfigProvider = serviceConfigProvider(configuredServices);
        ServiceConfigItem configItem = serviceConfigProvider.getConfig("service2");
        Set<String> serviceNames = serviceConfigProvider.getServiceNames();

        // then
        assertThat(configItem).isEqualTo(service2Config);
        assertThat(serviceNames).containsOnly("SERVICE1", "SERVICE2");
    }

    @Test
    @SuppressWarnings({"PMD.UnnecessaryFullyQualifiedName","PMD.DataflowAnomalyAnalysis"})
    void configShouldThrowExceptionWhenServiceIsNotConfigured() {
        ServiceConfigProvider serviceConfigProvider = serviceConfigProvider(
                Arrays.asList(
                        ServiceConfigHelper.serviceConfigItem(
                                "service",
                                singletonList("ctid"),
                                singletonList(ServiceConfigHelper.createCaseFieldMap("ACTOR_2", "pcqId2")),
                                null, null)
                )
        );

        assertThatThrownBy(
            () -> serviceConfigProvider.getConfig("non-existing-service")
        )
                .isInstanceOf(ServiceNotConfiguredException.class)
                .hasMessage("Service non-existing-service is not configured");
    }
}
