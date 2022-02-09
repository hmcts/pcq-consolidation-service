package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigProvider;
import uk.gov.hmcts.reform.pcqconsolidationservice.util.SpringBootIntegrationTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@TestPropertySource(locations = "/application.properties")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CcdClientApiTest extends SpringBootIntegrationTest {

    private static final String TEST_PCQ_ID = "455e6fe4-537a-4e82-9d1d-9a324465f2b5";
    private static final String TEST_DCN = "1657600014430175";
    private static final String TEST_SUFFIX_DCN = ".pdf";
    private static final String CASE_SEARCH_URL = "/searchCases";
    private static final Long EXPECTED_CASE_ID = 1_988_575L;
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_RESPONSE = "application/json;charset=UTF-8";
    private static final String WIREMOCK_TOKEN_ENDPOINT = "/o/token";
    private static final String WIREMOCK_LEASE_ENDPOINT = "/lease";
    private static final String WIREMOCK_DETAILS_ENDPOINT = "/details";
    private static final String WIREMOCK_TOKEN_RESULT = String.format(
            "{\"access_token\":\"TOKEN\",\"token_type\":\"Bearer\","
                    + "\"scope\": \"openid profile roles\",\"expires_in\":28800}");

    private static final String TEST_PROBATE_SERVICE_NAME = "PROBATE";
    private static final String TEST_PROBATE_CASE_FIELD_MAP_ACTOR_1 = "APPLICANT";
    private static final String TEST_PROBATE_EXPECTED_ES_STRING =
            "{\"query\": { \"match_phrase\" : { \"data.pcqId\" : \"" + TEST_PCQ_ID + "\" }}}";
    private static final String TEST_PROBATE_EXPECTED_DCN_ES_STRING =
            "{\"query\": { \"match_phrase\" : { \"data.scannedDocuments.value.controlNumber\" : \""
                    + TEST_DCN + "\" }}}";

    private static final String TEST_DIVORCE_SERVICE_NAME = "DIVORCE";
    private static final String TEST_DIVORCE_CASE_FIELD_MAP_ACTOR_1 = "PETITIONER";
    private static final String TEST_DIVORCE_EXPECTED_ES_STRING =
            "{\"query\": { \"match_phrase\" : { \"data.PetitionerPcqId\" : \"" + TEST_PCQ_ID + "\" }}}";

    private static final String TEST_CMC_SERVICE_NAME = "CMC";
    private static final String TEST_CMC_CASE_FIELD_MAP_ACTOR_1 = "DEFENDANT";
    private static final String TEST_CMC_EXPECTED_ES_STRING =
            "{\"query\": { \"match_phrase\" : { \"data.respondents.value.pcqId\" : \"" + TEST_PCQ_ID + "\" }}}";

    private static final String TEST_SSCS_SERVICE_NAME = "SSCS";
    private static final String TEST_SSCS_CASE_FIELD_MAP_ACTOR_1 = "APPELLANT";
    private static final String TEST_SSCS_EXPECTED_ES_STRING =
            "{\"query\": { \"match_phrase\" : { \"data.pcqId\" : \"" + TEST_PCQ_ID + "\" }}}";
    private static final String TEST_SSCS_EXPECTED_DCN_ES_STRING =
            "{\"query\": { \"match_phrase\" : { \"data.sscsDocument.value.documentFileName\" : \""
                    + TEST_DCN + TEST_SUFFIX_DCN + "\" }}}";

    @Autowired
    private CcdAuthenticatorFactory authenticatorFactory;

    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    @Autowired
    private ServiceConfigProvider serviceConfigProvider;

    @ClassRule
    public static WireMockClassRule wireMockServer = new WireMockClassRule(
            WireMockConfiguration.options().port(4554));


    @Test
    public void testProbatePcqWithoutCaseExecuteSuccess() {
        searchCasesMockSuccess(TEST_PROBATE_EXPECTED_ES_STRING);

        CcdClientApi ccdClientApi = new CcdClientApi(coreCaseDataApi, authenticatorFactory, serviceConfigProvider);
        List<Long> response = ccdClientApi.getCaseRefsByPcqId(
                TEST_PCQ_ID,
                TEST_PROBATE_SERVICE_NAME,
                TEST_PROBATE_CASE_FIELD_MAP_ACTOR_1);

        Assert.assertEquals(1, response.size());
        Assert.assertEquals(EXPECTED_CASE_ID, response.get(0));

        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_TOKEN_ENDPOINT)));
        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_LEASE_ENDPOINT)));
        WireMock.verify(1,getRequestedFor(urlEqualTo(WIREMOCK_DETAILS_ENDPOINT)));
    }

    @Test
    public void testProbateDcnSearchExecuteSuccess() {
        searchCasesMockSuccess(TEST_PROBATE_EXPECTED_DCN_ES_STRING);

        CcdClientApi ccdClientApi = new CcdClientApi(coreCaseDataApi, authenticatorFactory, serviceConfigProvider);
        List<Long> response = ccdClientApi.getCaseRefsByOriginatingFormDcn(
                TEST_DCN,
                TEST_PROBATE_SERVICE_NAME);

        Assert.assertEquals(1, response.size());
        Assert.assertEquals(EXPECTED_CASE_ID, response.get(0));

        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_TOKEN_ENDPOINT)));
        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_LEASE_ENDPOINT)));
        WireMock.verify(1,getRequestedFor(urlEqualTo(WIREMOCK_DETAILS_ENDPOINT)));
    }

    @Test
    public void testDivorcePcqWithoutCaseExecuteSuccess() {
        searchCasesMockSuccess(TEST_DIVORCE_EXPECTED_ES_STRING);

        CcdClientApi ccdClientApi = new CcdClientApi(coreCaseDataApi, authenticatorFactory, serviceConfigProvider);
        List<Long> response = ccdClientApi.getCaseRefsByPcqId(
                TEST_PCQ_ID,
                TEST_DIVORCE_SERVICE_NAME,
                TEST_DIVORCE_CASE_FIELD_MAP_ACTOR_1);

        Assert.assertEquals(1, response.size());
        Assert.assertEquals(EXPECTED_CASE_ID, response.get(0));

        WireMock.verify(3,postRequestedFor(urlEqualTo(WIREMOCK_TOKEN_ENDPOINT)));
        WireMock.verify(3,postRequestedFor(urlEqualTo(WIREMOCK_LEASE_ENDPOINT)));
        WireMock.verify(3,getRequestedFor(urlEqualTo(WIREMOCK_DETAILS_ENDPOINT)));
    }

    @Test
    public void testCmcPcqWithoutCaseExecuteSuccess() {
        searchCasesMockSuccess(TEST_CMC_EXPECTED_ES_STRING);

        CcdClientApi ccdClientApi = new CcdClientApi(coreCaseDataApi, authenticatorFactory, serviceConfigProvider);
        List<Long> response = ccdClientApi.getCaseRefsByPcqId(
                TEST_PCQ_ID,
                TEST_CMC_SERVICE_NAME,
                TEST_CMC_CASE_FIELD_MAP_ACTOR_1);

        Assert.assertEquals(1, response.size());
        Assert.assertEquals(EXPECTED_CASE_ID, response.get(0));

        WireMock.verify(2,postRequestedFor(urlEqualTo(WIREMOCK_TOKEN_ENDPOINT)));
        WireMock.verify(2,postRequestedFor(urlEqualTo(WIREMOCK_LEASE_ENDPOINT)));
        WireMock.verify(2,getRequestedFor(urlEqualTo(WIREMOCK_DETAILS_ENDPOINT)));
    }

    @Test
    public void testSscsPcqWithoutCaseExecuteSuccess() {
        searchCasesMockSuccess(TEST_SSCS_EXPECTED_ES_STRING);

        CcdClientApi ccdClientApi = new CcdClientApi(coreCaseDataApi, authenticatorFactory, serviceConfigProvider);
        List<Long> response = ccdClientApi.getCaseRefsByPcqId(
                TEST_PCQ_ID,
                TEST_SSCS_SERVICE_NAME,
                TEST_SSCS_CASE_FIELD_MAP_ACTOR_1);

        Assert.assertEquals(1, response.size());
        Assert.assertEquals(EXPECTED_CASE_ID, response.get(0));

        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_TOKEN_ENDPOINT)));
        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_LEASE_ENDPOINT)));
        WireMock.verify(1,getRequestedFor(urlEqualTo(WIREMOCK_DETAILS_ENDPOINT)));
    }

    @Test
    public void testSscsDcnSearchExecuteSuccess() {
        searchCasesMockSuccess(TEST_SSCS_EXPECTED_DCN_ES_STRING);

        CcdClientApi ccdClientApi = new CcdClientApi(coreCaseDataApi, authenticatorFactory, serviceConfigProvider);
        List<Long> response = ccdClientApi.getCaseRefsByOriginatingFormDcn(
                TEST_DCN,
                TEST_SSCS_SERVICE_NAME);

        Assert.assertEquals(1, response.size());
        Assert.assertEquals(EXPECTED_CASE_ID, response.get(0));

        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_TOKEN_ENDPOINT)));
        WireMock.verify(1,postRequestedFor(urlEqualTo(WIREMOCK_LEASE_ENDPOINT)));
        WireMock.verify(1,getRequestedFor(urlEqualTo(WIREMOCK_DETAILS_ENDPOINT)));
    }

    public static String fileContentAsString(String file) {
        return new String(fileContentAsBytes(file), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public static byte[] fileContentAsBytes(String file) {
        try {
            return Resources.toByteArray(Resources.getResource(file));
        } catch (IOException e) {
            throw new RuntimeException("Could not load file" + file, e);
        }
    }

    private void searchCasesMockSuccess(String expectedElasticSearchString) {
        wireMockServer.stubFor(post(WIREMOCK_TOKEN_ENDPOINT)
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_HEADER, JSON_RESPONSE)
                        .withStatus(200)
                        .withBody(WIREMOCK_TOKEN_RESULT)));

        wireMockServer.stubFor(post(urlPathMatching(CASE_SEARCH_URL))
                .withRequestBody(equalTo(expectedElasticSearchString))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_HEADER, JSON_RESPONSE)
                        .withStatus(200)
                        .withBody(fileContentAsString("ccd/searchCases/successful-response.json"))));

        wireMockServer.stubFor(get(urlPathMatching(WIREMOCK_DETAILS_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_HEADER, JSON_RESPONSE)
                        .withStatus(200)
                        .withBody("{\"user_details\":\"user details\"}")));

        wireMockServer.stubFor(post(urlPathMatching(WIREMOCK_LEASE_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_HEADER, JSON_RESPONSE)
                        .withStatus(200)
                        .withBody("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwcm9iYXRlX2Zyb250ZW5kIiwiZXhwIjoxNTg4NTM2NjI2fQ."
                                + "UFtuWsLC2eGWa5WAuZ_Vqxe3PODACpe-1b0xz-wmhx_wsY3urJROLW1E5Fh6Goh_yrT67UdG3oTSHTxn"
                                + "ojdUkg")));
    }
}

