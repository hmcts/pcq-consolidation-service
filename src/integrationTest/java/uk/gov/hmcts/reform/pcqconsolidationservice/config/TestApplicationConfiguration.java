package uk.gov.hmcts.reform.pcqconsolidationservice.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackages = {"uk.gov.hmcts.reform"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationRunner.class)
    }
)
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.pcq.commons"})
@EnableAutoConfiguration
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestApplicationConfiguration {

}
