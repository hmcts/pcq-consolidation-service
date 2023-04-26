package uk.gov.hmcts.reform.pcqconsolidationservice;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;


@SpringBootApplication(scanBasePackages = "uk.gov.hmcts.reform")
@EnableConfigurationProperties
@EnableFeignClients(basePackages = {
        "uk.gov.hmcts.reform.pcq.commons"
})
@Slf4j
public class ConsolidationApplication implements ApplicationRunner {

    @Autowired
    private ConsolidationComponent consolidationComponent;

    @Autowired
    private TelemetryClient client;

    @Value("${telemetry.wait.period:10000}")
    private int waitPeriod;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        try {
            log.info("Starting the consolidation service job");
            consolidationComponent.execute();
            log.info("Completed the consolidation service job successfully");
        } catch (Exception e) {
            log.error("Error executing Consolidation service", e);
        } finally {
            client.flush();
            waitTelemetryGracefulPeriod();
        }

    }

    private void waitTelemetryGracefulPeriod() throws InterruptedException {
        Thread.sleep(waitPeriod);
    }

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ConsolidationApplication.class);
        SpringApplication.exit(context);
    }
}