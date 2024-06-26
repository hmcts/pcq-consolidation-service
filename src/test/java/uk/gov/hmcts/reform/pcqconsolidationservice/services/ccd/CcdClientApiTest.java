package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigHelper;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigItem;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigProvider;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd.CcdClientApi.ES_MATCH_PHRASE_QUERY_FORMAT;

@ExtendWith(MockitoExtension.class)
class CcdClientApiTest {

    private static final String SERVICE = "pcqtest";
    private static final String CASE_TYPE_ID = "caseTypeA";
    private static final Long CASE_REF = 123_123_123L;
    private static final String PCQ_ID = "1234";
    private static final String DCN = "6789";
    private static final String ACTOR = "applicant";
    private static final String DEFAULT_PCQID_FIELD = "pcqId";
    private static final String DEFAULT_DCN_FIELD = "scannedDocuments.value.controlNumber";
    private static final String APPLICANT_PCQID_FIELD = "applicantPcdId";
    private static final String SSCS_DCN_FIELD = "sscsDocument.value.documentFileName";
    private static final String SSCS_DCN_DOCUMENT_SUFFIX = ".pdf";
    private static final String ERROR_MSG_PREFIX = "Test failed because of exception during execution. Message is ";

    public static final String SERVICE_TOKEN = "SERVICE_TOKEN";
    public static final String USER_TOKEN = "USER_TOKEN";
    public static final String USER_ID = "USER_ID";
    public static final String SEARCH_CASES_DEFAULT_PCQ_FIELD_SEARCH_STRING
            = format(ES_MATCH_PHRASE_QUERY_FORMAT, DEFAULT_PCQID_FIELD, PCQ_ID);
    public static final String SEARCH_CASES_APPLICANT_PCQ_FIELD_SEARCH_STRING
            = format(ES_MATCH_PHRASE_QUERY_FORMAT, APPLICANT_PCQID_FIELD, PCQ_ID);
    public static final String SEARCH_CASES_DEFAULT_DCN_FIELD_SEARCH_STRING
            = format(ES_MATCH_PHRASE_QUERY_FORMAT, DEFAULT_DCN_FIELD, DCN);
    public static final String SEARCH_CASES_CUSTOM_DCN_FIELD_SEARCH_STRING
            = format(ES_MATCH_PHRASE_QUERY_FORMAT, SSCS_DCN_FIELD, DCN + SSCS_DCN_DOCUMENT_SUFFIX);
    public static final UserInfo USER_INFO = UserInfo.builder().uid(USER_ID).build();

    public static final CcdAuthenticator AUTH_DETAILS = new CcdAuthenticator(
        () -> SERVICE_TOKEN,
            USER_INFO,
        () -> USER_TOKEN
    );

    @Mock
    private CoreCaseDataApi feignCcdApi;

    @Mock
    private CcdAuthenticatorFactory authenticatorFactory;

    @Mock
    private ServiceConfigProvider serviceConfigProvider;

    private CcdClientApi testCcdClientApi;

    private ServiceConfigItem serviceConfigWithCustomCcdFieldMapping;

    private ServiceConfigItem serviceConfigWithCustomDcnFieldMapping;

    private ServiceConfigItem serviceConfigWithMissingIncorrectActorCcdFieldMapping;

    private ServiceConfigItem serviceConfigNoCaseTypesMapping;

    private final CaseDetails caseDetail = CaseDetails.builder().id(CASE_REF).build();

    private final List<CaseDetails> caseDetailsList = Arrays.asList(new CaseDetails[]{caseDetail});

    private final SearchResult singleSearchResult = SearchResult.builder().total(1).cases(caseDetailsList).build();

    private final SearchResult emptySearchResult = SearchResult.builder().total(0).cases(emptyList()).build();

    @BeforeEach
    void setUp() {
        serviceConfigWithCustomCcdFieldMapping =
                ServiceConfigHelper.serviceConfigItem(
                        SERVICE,
                        singletonList(CASE_TYPE_ID),
                        singletonList(ServiceConfigHelper.createCaseFieldMap(ACTOR, APPLICANT_PCQID_FIELD)),
                        null, null);

        serviceConfigWithCustomDcnFieldMapping =
                ServiceConfigHelper.serviceConfigItem(
                        SERVICE,
                        singletonList(CASE_TYPE_ID),
                        emptyList(),
                        SSCS_DCN_FIELD, SSCS_DCN_DOCUMENT_SUFFIX);

        serviceConfigWithMissingIncorrectActorCcdFieldMapping =
                ServiceConfigHelper.serviceConfigItem(
                        SERVICE,
                        singletonList(CASE_TYPE_ID),
                        null, null, null);

        serviceConfigNoCaseTypesMapping =
                ServiceConfigHelper.serviceConfigItem(
                        SERVICE,
                        emptyList(),
                        singletonList(ServiceConfigHelper.createCaseFieldMap(ACTOR, APPLICANT_PCQID_FIELD)),
                        null, null);
    }

    @Test
    void useCcdClientToFindCasesByPcqIdWithNoPcqFieldMapping() {
        try {
            when(authenticatorFactory.createCcdAuthenticator()).thenReturn(AUTH_DETAILS);
            when(serviceConfigProvider.getConfig(anyString()))
                    .thenReturn(serviceConfigWithMissingIncorrectActorCcdFieldMapping);
            when(feignCcdApi.searchCases(USER_TOKEN, SERVICE_TOKEN, CASE_TYPE_ID,
                    SEARCH_CASES_DEFAULT_PCQ_FIELD_SEARCH_STRING)).thenReturn(singleSearchResult);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            List<Long> response = testCcdClientApi.getCaseRefsByPcqId(PCQ_ID, SERVICE, ACTOR);
            Assert.assertEquals("Search find correct number of cases", 1, response.size());
            Assert.assertEquals("Search find correct case with pcqId field", CASE_REF, response.get(0));
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }

    @Test
    void useCcdClientToFindCasesByDcn() {
        try {
            when(authenticatorFactory.createCcdAuthenticator()).thenReturn(AUTH_DETAILS);
            when(serviceConfigProvider.getConfig(anyString()))
                    .thenReturn(serviceConfigWithCustomCcdFieldMapping);
            when(feignCcdApi.searchCases(USER_TOKEN, SERVICE_TOKEN, CASE_TYPE_ID,
                    SEARCH_CASES_DEFAULT_DCN_FIELD_SEARCH_STRING)).thenReturn(singleSearchResult);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            List<Long> response = testCcdClientApi.getCaseRefsByOriginatingFormDcn(DCN, SERVICE);
            Assert.assertEquals("Search find correct number of cases", 1, response.size());
            Assert.assertEquals("Search find correct case with dcn field", CASE_REF, response.get(0));
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }

    @Test
    void useCcdClientWithCachedAuthentication() {
        try {
            when(serviceConfigProvider.getConfig(anyString()))
                    .thenReturn(serviceConfigWithMissingIncorrectActorCcdFieldMapping);
            when(feignCcdApi.searchCases(
                    USER_TOKEN, SERVICE_TOKEN, CASE_TYPE_ID,
                    SEARCH_CASES_DEFAULT_PCQ_FIELD_SEARCH_STRING)).thenReturn(singleSearchResult);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            ReflectionTestUtils.setField(testCcdClientApi, // inject into this object
                    "authenticator", // assign to this field
                    AUTH_DETAILS); // object to be injected

            List<Long> response = testCcdClientApi.getCaseRefsByPcqId(PCQ_ID, SERVICE, ACTOR);
            Assert.assertEquals("Search find correct number of cases with cached auth", 1, response.size());
            Assert.assertEquals("Search find correct case with cached auth", CASE_REF, response.get(0));
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }

    @Test
    void useCcdClientToFindCasesByPcqIdWithCustomPcqField() {
        try {
            when(authenticatorFactory.createCcdAuthenticator()).thenReturn(AUTH_DETAILS);
            when(serviceConfigProvider.getConfig(anyString())).thenReturn(serviceConfigWithCustomCcdFieldMapping);
            when(feignCcdApi.searchCases(
                    USER_TOKEN, SERVICE_TOKEN, CASE_TYPE_ID,
                    SEARCH_CASES_APPLICANT_PCQ_FIELD_SEARCH_STRING)).thenReturn(singleSearchResult);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            List<Long> response = testCcdClientApi.getCaseRefsByPcqId(PCQ_ID, SERVICE, ACTOR);
            Assert.assertEquals("Search find correct number of cases with custom pcqId field", 1, response.size());
            Assert.assertEquals("Search find correct case with custom pcqId field", CASE_REF, response.get(0));
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }

    @Test
    void useCcdClientToFindCasesByDcnWithCustomDcnField() {
        try {
            when(authenticatorFactory.createCcdAuthenticator()).thenReturn(AUTH_DETAILS);
            when(serviceConfigProvider.getConfig(anyString()))
                    .thenReturn(serviceConfigWithCustomDcnFieldMapping);
            when(feignCcdApi.searchCases(
                    USER_TOKEN, SERVICE_TOKEN, CASE_TYPE_ID,
                    SEARCH_CASES_CUSTOM_DCN_FIELD_SEARCH_STRING)).thenReturn(singleSearchResult);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            List<Long> response = testCcdClientApi.getCaseRefsByOriginatingFormDcn(DCN, SERVICE);
            Assert.assertEquals("Search find correct number of cases", 1, response.size());
            Assert.assertEquals("Search find correct case with dcn field", CASE_REF, response.get(0));
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }

    @Test
    void useCcdClientButNoMatchesAreReturned() {
        try {
            when(authenticatorFactory.createCcdAuthenticator()).thenReturn(AUTH_DETAILS);
            when(serviceConfigProvider.getConfig(anyString())).thenReturn(serviceConfigWithCustomCcdFieldMapping);
            when(feignCcdApi.searchCases(
                    USER_TOKEN, SERVICE_TOKEN, CASE_TYPE_ID,
                    SEARCH_CASES_APPLICANT_PCQ_FIELD_SEARCH_STRING)).thenReturn(emptySearchResult);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            List<Long> response = testCcdClientApi.getCaseRefsByPcqId(PCQ_ID, SERVICE, ACTOR);
            Assert.assertEquals("Should be no cases if match is not made", 0, response.size());
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }

    @Test
    void useCcdClientButNoCaseTypeIdsMatchForPcqIdSearch() {
        try {
            when(authenticatorFactory.createCcdAuthenticator()).thenReturn(AUTH_DETAILS);
            when(serviceConfigProvider.getConfig(anyString())).thenReturn(serviceConfigNoCaseTypesMapping);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            List<Long> response = testCcdClientApi.getCaseRefsByPcqId(PCQ_ID, SERVICE, ACTOR);
            Assert.assertEquals("Should be no cases if case types are not found", 0, response.size());
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }

    @Test
    void useCcdClientButNoCaseTypeIdsMatchForDcnSearch() {
        try {
            when(authenticatorFactory.createCcdAuthenticator()).thenReturn(AUTH_DETAILS);
            when(serviceConfigProvider.getConfig(anyString())).thenReturn(serviceConfigNoCaseTypesMapping);

            testCcdClientApi = new CcdClientApi(feignCcdApi, authenticatorFactory, serviceConfigProvider);
            List<Long> response = testCcdClientApi.getCaseRefsByOriginatingFormDcn(DCN, SERVICE);
            Assert.assertEquals("Should be no cases if case types are not found", 0, response.size());
        } catch (Exception e) {
            fail(ERROR_MSG_PREFIX + e.getMessage());
        }
    }
}
