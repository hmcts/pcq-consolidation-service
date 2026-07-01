package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import org.junit.jupiter.api.Assertions;
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

    private static final String SERVICE_TOKEN = "SERVICE_TOKEN";
    private static final String USER_TOKEN = "123456789";

    private static final String IDAM_USERS_PCQ_USERNAME = "pcq@gmail.com";
    private static final String IDAM_USERS_PCQ_PASSWORD = "password1234";

    private static final String USER_ID = "pcq";

    private static final UserInfo USER_INFO = UserInfo.builder().uid(USER_ID).build();

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

        Assertions.assertEquals(SERVICE_TOKEN, authenticator.getServiceToken(), "Service Token Not Matching");
        Assertions.assertEquals(USER_TOKEN, authenticator.getUserToken(), "User Token not matching");
        Assertions.assertEquals(USER_ID, authenticator.getUserId(), "User Id Not matching");
        Assertions.assertTrue(authenticator.userTokenAgeInSeconds() > 0, "User Token not valid");
    }

    @Test
    void returnSuccessfulCredentials() {
        Credential user = new Credential(IDAM_USERS_PCQ_USERNAME, IDAM_USERS_PCQ_PASSWORD);

        Assertions.assertEquals(IDAM_USERS_PCQ_USERNAME, user.getUsername(), "Idam UserName not matching");
        Assertions.assertEquals(IDAM_USERS_PCQ_PASSWORD, user.getPassword(), "Idam password not matching");
    }

}
