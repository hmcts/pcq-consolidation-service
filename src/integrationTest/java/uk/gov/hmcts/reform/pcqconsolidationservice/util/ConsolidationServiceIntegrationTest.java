package uk.gov.hmcts.reform.pcqconsolidationservice.util;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {"PCQ_BACKEND_URL:http://127.0.0.1:4554"})
@SuppressWarnings("PMD.TooManyMethods")
class ConsolidationServiceIntegrationTest extends SpringBootIntegrationTest {

    private static final String CASE_ID_TEST = "TEST_CASE_ID";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String MEDIA_TYPE = "application/json";
    private static final String CONNECTION_HEADER_VAL = "close";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String TEST_SERVICE_AUTHORIZATION = "Bearer test-s2s";

    @RegisterExtension
    static final WireMockExtension PCQ_BACKEND_SERVICE =
            WireMockExtension.newInstance()
                    .options(WireMockConfiguration.wireMockConfig().port(4554))
                    .build();

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(TEST_SERVICE_AUTHORIZATION);
    }

    @Test
    void testAddCaseForPcqExecuteSuccess() {
        pcqAddCaseWireMockSuccess();

        ResponseEntity responseEntity = pcqBackendServiceImpl.addCaseForPcq("TEST_PCQ_ID", CASE_ID_TEST);
        Assertions.assertNotNull(responseEntity, "");
    }

    @Test
    void testAddCaseForPcqExecuteInvalidRequest() {
        pcqAddCaseWireMockInvalidRequest();

        ResponseEntity responseEntity = pcqBackendServiceImpl.addCaseForPcq("TEST_PCQ_ID", CASE_ID_TEST);
        Assertions.assertNotNull(responseEntity, "");
    }

    @Test
    void testAddCaseForPcqExecuteInternalError() {
        pcqAddCaseWireMockInternalError();

        ResponseEntity responseEntity = pcqBackendServiceImpl.addCaseForPcq("TEST_PCQ_ID", CASE_ID_TEST);
        Assertions.assertNotNull(responseEntity, "");
    }

    @Test
    void testPcqWithoutCaseExecuteSuccess() {
        pcqWithoutCaseWireMockSuccess();

        ResponseEntity responseEntity = pcqBackendServiceImpl.getPcqWithoutCase();
        Assertions.assertNotNull(responseEntity, "");
    }

    @Test
    void testPcqWithoutCaseExecuteInvalidError() {
        pcqWithoutCaseWireMockFailure();

        ResponseEntity responseEntity = pcqBackendServiceImpl.getPcqWithoutCase();
        Assertions.assertNotNull(responseEntity, "");
    }

    @Test
    void testPcqWithoutCaseExecuteInternalError() {
        pcqWithoutCaseWireMockInternalError();

        ResponseEntity responseEntity = pcqBackendServiceImpl.getPcqWithoutCase();
        Assertions.assertNotNull(responseEntity, "");
    }

    private void pcqWithoutCaseWireMockSuccess() {
        PCQ_BACKEND_SERVICE.stubFor(get(urlPathMatching("/pcq/backend/consolidation/pcqRecordWithoutCase"))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(TEST_SERVICE_AUTHORIZATION))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                        .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                        .withStatus(200)
                        .withBody("{"
                                + "    \"pcqRecord\": ["
                                + "        {"
                                + "            \"pcqAnswers\": null,"
                                + "            \"pcqId\": \"d1bc52bc-b673-46d3-a0d8-052ef678772e\","
                                + "            \"ccdCaseId\": null,"
                                + "            \"partyId\": null,"
                                + "            \"channel\": null,"
                                + "            \"completedDate\": null,"
                                + "            \"serviceId\": \"PROBATE_TEST\","
                                + "            \"actor\": \"DEFENDANT\","
                                + "            \"versionNo\": null"
                                + "        },"
                                + "        {"
                                + "            \"pcqAnswers\": null,"
                                + "            \"pcqId\": \"27f29282-6ff5-4a06-9277-fea8058a07a9\","
                                + "            \"ccdCaseId\": null,"
                                + "            \"partyId\": null,"
                                + "            \"channel\": null,"
                                + "            \"completedDate\": null,"
                                + "            \"serviceId\": \"PROBATE_TEST\","
                                + "            \"actor\": \"DEFENDANT\","
                                + "            \"versionNo\": null"
                                + "        }"
                                + "    ],"
                                + "    \"responseStatus\": \"Success\","
                                + "    \"responseStatusCode\": \"200\""
                                + "}")));
    }



    private void pcqAddCaseWireMockSuccess() {
        PCQ_BACKEND_SERVICE.stubFor(put(urlPathMatching("/pcq/backend/consolidation/addCaseForPCQ/TEST_PCQ_ID"))
                .withQueryParam("caseId", equalTo(CASE_ID_TEST))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(TEST_SERVICE_AUTHORIZATION))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                        .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                        .withStatus(200)
                        .withBody("{\"pcqId\": \"TEST_PCQ_ID\","
                                + "\"responseStatus\": \"Success\","
                                + "\"responseStatusCode\": \"200\"}")));
    }

    private void pcqWithoutCaseWireMockFailure() {
        PCQ_BACKEND_SERVICE.stubFor(get(urlPathMatching("/pcq/backend/consolidation/pcqRecordWithoutCase"))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(TEST_SERVICE_AUTHORIZATION))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                        .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                        .withStatus(400)
                        .withBody("{\"responseStatus\": \"Invalid Request\","
                                + "\"responseStatusCode\": \"400\"}")));
    }

    private void pcqWithoutCaseWireMockInternalError() {
        PCQ_BACKEND_SERVICE.stubFor(get(urlPathMatching("/pcq/backend/consolidation/pcqRecordWithoutCase"))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(TEST_SERVICE_AUTHORIZATION))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                        .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                        .withStatus(500)
                        .withBody("{\"responseStatus\": \"Unknown error occurred\","
                                + "\"responseStatusCode\": \"500\"}")));
    }

    private void pcqAddCaseWireMockInvalidRequest() {
        PCQ_BACKEND_SERVICE.stubFor(put(urlPathMatching("/pcq/backend/consolidation/addCaseForPCQ/TEST_PCQ_ID"))
                .withQueryParam("caseId", equalTo(CASE_ID_TEST))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(TEST_SERVICE_AUTHORIZATION))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                        .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                        .withStatus(400)
                        .withBody("{\"pcqId\": \"TEST_PCQ_ID\","
                                + "\"responseStatus\": \"Invalid Request\","
                                + "\"responseStatusCode\": \"400\"}")));
    }

    private void pcqAddCaseWireMockInternalError() {
        PCQ_BACKEND_SERVICE.stubFor(put(urlPathMatching("/pcq/backend/consolidation/addCaseForPCQ/TEST_PCQ_ID"))
                .withQueryParam("caseId", equalTo(CASE_ID_TEST))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(TEST_SERVICE_AUTHORIZATION))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                        .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                        .withStatus(500)
                        .withBody("{\"pcqId\": \"TEST_PCQ_ID\","
                                + "\"responseStatus\": \"Unknown error occurred\","
                                + "\"responseStatusCode\": \"500\"}")));
    }




}
