package uk.gov.hmcts.reform.pcqconsolidationservice.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcq.commons.controller.feign.PcqBackendFeignClient;
import uk.gov.hmcts.reform.pcqconsolidationservice.ConsolidationComponent;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.TestApplicationConfiguration;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.pcqbackend.impl.PcqBackendServiceImpl;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplicationConfiguration.class)
@SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod", "PMD.AbstractClassWithoutAbstractMethod"})
public abstract class SpringBootIntegrationTest {

    @Autowired
    protected PcqBackendServiceImpl pcqBackendServiceImpl;

    @Autowired
    protected PcqBackendFeignClient pcqBackendFeignClient;

    @Autowired
    protected ConsolidationComponent consolidationComponent;

}
