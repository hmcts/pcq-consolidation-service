package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class CcdAuthenticator {

    private final UserInfo userInfo;
    private final Supplier<String> serviceTokenSupplier;
    private final Supplier<String> userTokenSupplier;
    private final Instant userTokenCreationTime;

    public CcdAuthenticator(
            Supplier<String> serviceTokenSupplier,
            UserInfo userInfo,
            Supplier<String> userTokenSupplier
    ) {
        this.serviceTokenSupplier = serviceTokenSupplier;
        this.userInfo = userInfo;
        this.userTokenSupplier = userTokenSupplier;
        this.userTokenCreationTime = Instant.now();
    }

    public String getUserToken() {
        return this.userTokenSupplier.get();
    }

    public String getServiceToken() {
        return this.serviceTokenSupplier.get();
    }

    public String getUserId() {
        return this.userInfo.getUid();
    }

    public long userTokenAgeInSeconds() {
        return Math.abs(Duration.between(Instant.now(),userTokenCreationTime).toSeconds());
    }
}
