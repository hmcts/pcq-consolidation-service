package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

public class CcdAuthenticator {

    private final UserInfo userInfo;
    private final Supplier<String> serviceTokenSupplier;
    private final Supplier<String> userTokenSupplier;
    private final LocalDateTime userTokenCreationDate;

    public CcdAuthenticator(
            Supplier<String> serviceTokenSupplier,
            UserInfo userInfo,
            Supplier<String> userTokenSupplier
    ) {
        this.serviceTokenSupplier = serviceTokenSupplier;
        this.userInfo = userInfo;
        this.userTokenSupplier = userTokenSupplier;
        this.userTokenCreationDate = LocalDateTime.now();
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
        LocalDateTime now = LocalDateTime.now();
        Duration dur = Duration.between(now, userTokenCreationDate);
        return Math.abs(dur.toSeconds());
    }
}
