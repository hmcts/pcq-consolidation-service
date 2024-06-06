package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.idam.Credential;

@Service
public class CcdAuthenticatorFactory {

    private final AuthTokenGenerator s2sTokenGenerator;
    private final IdamClient idamClient;

    @Value("${idam.users.pcq.username}")
    private String pcqCcdExtractorUserName;

    @Value("${idam.users.pcq.password}")
    private String pcqCcdExtractorPassword;

    public CcdAuthenticatorFactory(
            AuthTokenGenerator s2sTokenGenerator,
            IdamClient idamClient
    ) {
        this.s2sTokenGenerator = s2sTokenGenerator;
        this.idamClient = idamClient;
    }

    public CcdAuthenticator createCcdAuthenticator() {
        Credential user = new Credential(pcqCcdExtractorUserName, pcqCcdExtractorPassword);
        String accessToken = idamClient.getAccessToken(user.getUsername(), user.getPassword());
        UserInfo userInfo = idamClient.getUserInfo(accessToken);
        return new CcdAuthenticator(
                s2sTokenGenerator::generate,
                userInfo, () -> accessToken
        );
    }
}
