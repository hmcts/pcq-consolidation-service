package uk.gov.hmcts.reform.pcqconsolidationservice;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;


@SpringBootApplication(scanBasePackages = "uk.gov.hmcts.reform")
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.pcq.commons"})
@Slf4j
@RequiredArgsConstructor
public class ConsolidationApplication implements ApplicationRunner {

    private final ConsolidationComponent consolidationComponent;

    private final TelemetryClient client;

    @Value("${telemetry.wait.period:10000}")
    private int waitPeriod;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        try {
            log.info("Starting the consolidation service job");
            consolidationComponent.execute();
            log.info("Completed the consolidation service job successfully");
        } catch (Exception e) {
            //To trace the log and create alert
            log.error("Error executing Consolidation service : " +  e);
            //To have stack trace
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
