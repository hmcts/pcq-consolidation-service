package uk.gov.hmcts.reform.pcqconsolidationservice.services.pcqbackend.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcq.commons.controller.feign.PcqBackendFeignClient;
import uk.gov.hmcts.reform.pcq.commons.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcq.commons.model.PcqRecordWithoutCaseResponse;
import uk.gov.hmcts.reform.pcq.commons.model.SubmitResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.advice.ErrorResponse;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.TooManyMethods")
class PcqBackendServiceImplTest {

    private final PcqBackendFeignClient mockPcqBackendFeignClient = mock(PcqBackendFeignClient.class);

    private final PcqBackendServiceImpl pcqBackendService = new PcqBackendServiceImpl(mockPcqBackendFeignClient);

    private static final String HEADER_VALUE = null;
    private static final String RESPONSE_INCORRECT = "Not the correct response";
    private static final int STATUS_OK = 200;
    private static final String TEST_PCQ_ID = "UNIT_TEST_PCQ_1";
    private static final String TEST_CASE_ID = "UNIT_TEST_CASE_1";
    private static final String EXPECTED_MSG_1 = "PcqIds don't match";
    private static final String EXPECTED_MSG_2 = "Status code not correct";
    private static final String EXPECTED_MSG_3 = "Status not correct";
    private static final PcqAnswerResponse[] EXPECT_EMPTY_PCQ_ANSWER_RESPONSE = {};

    @Test
    void testSuccess200Response() throws JsonProcessingException {
        PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = generateTestResponse("Success", 200);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(pcqWithoutCaseResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase(HEADER_VALUE)).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(200).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof PcqRecordWithoutCaseResponse);
        PcqRecordWithoutCaseResponse responseBody = (PcqRecordWithoutCaseResponse) responseEntity.getBody();
        assertPcqRecordsEqual(pcqWithoutCaseResponse.getPcqRecord(), responseBody.getPcqRecord());
        assertEquals(EXPECTED_MSG_2, pcqWithoutCaseResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals(EXPECTED_MSG_3, pcqWithoutCaseResponse.getResponseStatus(),
                responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase(HEADER_VALUE);

    }

    @Test
    void testSuccess200ResponseWithHeaders() throws JsonProcessingException {
        PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = generateTestResponse("Success", 200);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(pcqWithoutCaseResponse);
        Map<String, Collection<String>> headers = new ConcurrentHashMap<>();
        headers.put("Content-Type", Collections.singleton("application/json"));

        when(mockPcqBackendFeignClient.getPcqWithoutCase(HEADER_VALUE)).thenReturn(Response.builder().request(mock(
                Request.class)).headers(headers).body(body, Charset.defaultCharset()).status(200).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof PcqRecordWithoutCaseResponse);
        PcqRecordWithoutCaseResponse responseBody = (PcqRecordWithoutCaseResponse) responseEntity.getBody();
        assertPcqRecordsEqual(pcqWithoutCaseResponse.getPcqRecord(), responseBody.getPcqRecord());
        assertEquals(EXPECTED_MSG_2, pcqWithoutCaseResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals(EXPECTED_MSG_3, pcqWithoutCaseResponse.getResponseStatus(),
                responseBody.getResponseStatus());

        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase(HEADER_VALUE);
    }

    @Test
    void testSuccess200Response2() throws JsonProcessingException {
        SubmitResponse submitResponse = generateSubmitTestResponse("Success", 200);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(submitResponse);

        when(mockPcqBackendFeignClient.addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID)).thenReturn(
                Response.builder().request(mock(Request.class)).body(body, Charset.defaultCharset())
                        .status(200).build());

        ResponseEntity responseEntity = pcqBackendService.addCaseForPcq(TEST_PCQ_ID, TEST_CASE_ID);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof SubmitResponse);
        SubmitResponse responseBody = (SubmitResponse) responseEntity.getBody();
        assertEquals(EXPECTED_MSG_1, TEST_PCQ_ID, responseBody.getPcqId());
        assertEquals(EXPECTED_MSG_2, submitResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals(EXPECTED_MSG_3, submitResponse.getResponseStatus(),
                responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID);

    }

    @Test
    void testInvalidRequestErrorResponse() throws JsonProcessingException {
        PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = generateTestResponse("Invalid Request", 400);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(pcqWithoutCaseResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase(HEADER_VALUE)).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(400).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof PcqRecordWithoutCaseResponse);
        PcqRecordWithoutCaseResponse responseBody = (PcqRecordWithoutCaseResponse) responseEntity.getBody();
        assertEquals("PcqIds size don't match", 0, responseBody.getPcqRecord().length);
        assertEquals(EXPECTED_MSG_2, pcqWithoutCaseResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals(EXPECTED_MSG_3, pcqWithoutCaseResponse.getResponseStatus(),
                responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase(HEADER_VALUE);

    }

    @Test
    void testInvalidRequestErrorResponse2() throws JsonProcessingException {
        SubmitResponse submitResponse = generateSubmitTestResponse("Invalid Request", 400);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(submitResponse);

        when(mockPcqBackendFeignClient.addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID)).thenReturn(
                Response.builder().request(mock(Request.class)).body(body, Charset.defaultCharset())
                        .status(400).build());

        ResponseEntity responseEntity = pcqBackendService.addCaseForPcq(TEST_PCQ_ID, TEST_CASE_ID);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof SubmitResponse);
        SubmitResponse responseBody = (SubmitResponse) responseEntity.getBody();
        assertNull("PCQ_ID not expected here", responseBody.getPcqId());
        assertEquals(EXPECTED_MSG_2, submitResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals(EXPECTED_MSG_3, submitResponse.getResponseStatus(),
                responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID);

    }

    @Test
    void testUnknownErrorResponse() throws JsonProcessingException {
        PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = generateTestResponse("Unknown error occurred", 500);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(pcqWithoutCaseResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase(HEADER_VALUE)).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(500).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof PcqRecordWithoutCaseResponse);
        PcqRecordWithoutCaseResponse responseBody = (PcqRecordWithoutCaseResponse) responseEntity.getBody();
        assertEquals("PcqIds size don't match", 0, responseBody.getPcqRecord().length);
        assertEquals(EXPECTED_MSG_2, pcqWithoutCaseResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals(EXPECTED_MSG_3, pcqWithoutCaseResponse.getResponseStatus(),
                responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase(HEADER_VALUE);

    }

    @Test
    void testUnknownErrorResponse2() throws JsonProcessingException {
        SubmitResponse submitResponse = generateSubmitTestResponse("Unknown error occurred", 500);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(submitResponse);

        when(mockPcqBackendFeignClient.addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID)).thenReturn(
                Response.builder().request(mock(Request.class)).body(body, Charset.defaultCharset()).status(500)
                        .build());

        ResponseEntity responseEntity = pcqBackendService.addCaseForPcq(TEST_PCQ_ID, TEST_CASE_ID);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof SubmitResponse);
        SubmitResponse responseBody = (SubmitResponse) responseEntity.getBody();
        assertNull("PCQ_ID not expected here", responseBody.getPcqId());
        assertEquals(EXPECTED_MSG_2, submitResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals(EXPECTED_MSG_3, submitResponse.getResponseStatus(),
                responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID);

    }

    @Test
    void testOtherErrorResponse() throws JsonProcessingException {
        ErrorResponse errorResponse = generateErrorResponse();
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(errorResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase(HEADER_VALUE)).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(503).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof PcqRecordWithoutCaseResponse);
        PcqRecordWithoutCaseResponse responseBody = (PcqRecordWithoutCaseResponse) responseEntity.getBody();
        assertArrayEquals("", EXPECT_EMPTY_PCQ_ANSWER_RESPONSE, responseBody.getPcqRecord());
        assertNull("", responseBody.getResponseStatus());
        assertNull("", responseBody.getResponseStatusCode());

    }

    @Test
    void testOtherErrorResponse2() throws JsonProcessingException {
        ErrorResponse errorResponse = generateErrorResponse();
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(errorResponse);

        when(mockPcqBackendFeignClient.addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID)).thenReturn(Response
                .builder().request(mock(Request.class)).body(body, Charset.defaultCharset()).status(503).build());

        ResponseEntity responseEntity = pcqBackendService.addCaseForPcq(TEST_PCQ_ID, TEST_CASE_ID);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof SubmitResponse);
        SubmitResponse responseBody = (SubmitResponse) responseEntity.getBody();
        assertNull("", responseBody.getPcqId());
        assertNull("", responseBody.getResponseStatus());
        assertNull("", responseBody.getResponseStatusCode());

    }

    @Test
    void executeFeignApiError() {
        Map<String, Collection<String>> maps = new ConcurrentHashMap<>();
        FeignException feignException = new FeignException.BadGateway("Bade Gateway Error", mock(Request.class),
                "Test".getBytes(),maps);

        when(mockPcqBackendFeignClient.getPcqWithoutCase(HEADER_VALUE)).thenThrow(feignException);

        assertThrows(ExternalApiException.class, pcqBackendService::getPcqWithoutCase);

        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase(HEADER_VALUE);
    }

    @Test
    void executeIoErrorResponse() {
        String invalidBody = "not-json";
        when(mockPcqBackendFeignClient.getPcqWithoutCase(HEADER_VALUE)).thenReturn(Response.builder().request(mock(
                Request.class)).body(invalidBody, Charset.defaultCharset()).status(200).build());

        assertThrows(ExternalApiException.class, pcqBackendService::getPcqWithoutCase);

        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase(HEADER_VALUE);
    }

    @Test
    void executeFeignApiError2() {
        Map<String, Collection<String>> maps = new ConcurrentHashMap<>();
        FeignException feignException = new FeignException.BadGateway("Bade Gateway Error", mock(Request.class),
                "Test".getBytes(),maps);
        when(mockPcqBackendFeignClient.addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID,
                TEST_CASE_ID)).thenThrow(feignException);

        assertThrows(ExternalApiException.class, () -> pcqBackendService.addCaseForPcq(TEST_PCQ_ID, TEST_CASE_ID));

        verify(mockPcqBackendFeignClient, times(1)).addCaseForPcq(HEADER_VALUE, TEST_PCQ_ID, TEST_CASE_ID);
    }


    private PcqRecordWithoutCaseResponse generateTestResponse(String message, int statusCode) {
        PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = new PcqRecordWithoutCaseResponse();
        pcqWithoutCaseResponse.setResponseStatus(message);
        pcqWithoutCaseResponse.setResponseStatusCode(String.valueOf(statusCode));

        if (statusCode == STATUS_OK) {
            PcqAnswerResponse answerResponse1 = generateTestAnswer("PCQ_ID1", "SERVICE_JD1", "ACTOR_1");
            PcqAnswerResponse answerResponse2 = generateTestAnswer("PCQ_ID2", "SERVICE_JD2", "ACTOR_2");

            PcqAnswerResponse[] answerResponses = {answerResponse1, answerResponse2};
            pcqWithoutCaseResponse.setPcqRecord(answerResponses);
        }

        return pcqWithoutCaseResponse;
    }

    private SubmitResponse generateSubmitTestResponse(String message, int statusCode) {
        SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setResponseStatus(message);
        submitResponse.setResponseStatusCode(String.valueOf(statusCode));

        if (statusCode == STATUS_OK) {
            String pcqIds = "UNIT_TEST_PCQ_1";
            submitResponse.setPcqId(pcqIds);
        }

        return submitResponse;
    }

    private ErrorResponse generateErrorResponse() {

        return new ErrorResponse("Bad Gateway",
                "Server did not respond", LocalDateTime.now().toString());
    }

    private PcqAnswerResponse generateTestAnswer(String pcqId, String serviceId, String actor) {
        PcqAnswerResponse answerResponse = new PcqAnswerResponse();
        answerResponse.setPcqId(pcqId);
        answerResponse.setServiceId(serviceId);
        answerResponse.setActor(actor);

        return answerResponse;
    }

    @SuppressWarnings("PMD.UseVarargs")
    private void assertPcqRecordsEqual(PcqAnswerResponse[] originalAnswers, PcqAnswerResponse[] responseAnswers) {
        assertEquals("Pcq Answers Array Length don't match", originalAnswers.length, responseAnswers.length);
        for (int i = 0; i < originalAnswers.length; i++) {
            assertEquals("PCQ Ids don't match", originalAnswers[i].getPcqId(), responseAnswers[i].getPcqId());
            assertEquals("Service Ids don't match", originalAnswers[i].getServiceId(),
                    responseAnswers[i].getServiceId());
            assertEquals("Actors don't match", originalAnswers[i].getActor(), responseAnswers[i].getActor());
        }
    }
}
