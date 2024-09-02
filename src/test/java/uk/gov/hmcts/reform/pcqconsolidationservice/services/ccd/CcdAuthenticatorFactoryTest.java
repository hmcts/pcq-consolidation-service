package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.idam.Credential;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdAuthenticatorFactoryTest {

    public static final String SERVICE_TOKEN = "SERVICE_TOKEN";
    public static final String USER_TOKEN = "123456789";

    private static final String IDAM_USERS_PCQ_USERNAME = "pcq@gmail.com";
    private static final String IDAM_USERS_PCQ_PASSWORD = "password1234";

    private static final String USER_ID = "pcq";

    public static final UserInfo USER_INFO = UserInfo.builder().uid(USER_ID).build();

    @Mock
    private AuthTokenGenerator tokenGenerator;

    @Mock
    private IdamClient idamClient;

    @Test
    void returnSuccessfulCcdAuthenticator() {
        when(idamClient.getAccessToken(any(), any())).thenReturn(USER_TOKEN);
        when(idamClient.getUserInfo(any())).thenReturn(USER_INFO);
        when(tokenGenerator.generate()).thenReturn(SERVICE_TOKEN);

        CcdAuthenticatorFactory service = new CcdAuthenticatorFactory(tokenGenerator, idamClient);
        CcdAuthenticator authenticator = service.createCcdAuthenticator();

        Assert.assertEquals("Service Token Not Matching",SERVICE_TOKEN, authenticator.getServiceToken());
        Assert.assertEquals("User Token not matching", USER_TOKEN, authenticator.getUserToken());
        Assert.assertEquals("User Id Not matching", USER_ID, authenticator.getUserId());
        Assert.assertTrue("User Token not valid",authenticator.userTokenAgeInSeconds() > 0);
    }

    @Test
    void returnSuccessfulCredentials() {
        Credential user = new Credential(IDAM_USERS_PCQ_USERNAME, IDAM_USERS_PCQ_PASSWORD);

        Assert.assertEquals("Idam UserName not mattching",IDAM_USERS_PCQ_USERNAME, user.getUsername());
        Assert.assertEquals("Idam password not matching",IDAM_USERS_PCQ_PASSWORD, user.getPassword());
    }

}
