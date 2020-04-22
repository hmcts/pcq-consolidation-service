package uk.gov.hmcts.reform.pcqconsolidationservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.advice.ErrorResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.advice.ExternalApiException;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.feign.PcqBackendFeignClient;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.response.PcqWithoutCaseResponse;

import java.nio.charset.Charset;
import java.time.LocalDateTime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PcqBackendServiceImplTest {

    private final PcqBackendFeignClient mockPcqBackendFeignClient = mock(PcqBackendFeignClient.class);

    private final PcqBackendServiceImpl pcqBackendService = new PcqBackendServiceImpl(mockPcqBackendFeignClient);

    @Test
    public void testSuccess200Response() throws JsonProcessingException {
        PcqWithoutCaseResponse pcqWithoutCaseResponse = generateTestResponse("Success", 200);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(pcqWithoutCaseResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase()).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(200).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue( "Not the correct response", responseEntity.getBody() instanceof PcqWithoutCaseResponse);
        PcqWithoutCaseResponse responseBody = (PcqWithoutCaseResponse) responseEntity.getBody();
        assertArrayEquals("PcqIds don't match", pcqWithoutCaseResponse.getPcqId(), responseBody.getPcqId());
        assertEquals("Status code not correct", pcqWithoutCaseResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals("Status not correct", pcqWithoutCaseResponse.getResponseStatus(), responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase();

    }

    @Test
    public void testInvalidRequestErrorResponse() throws JsonProcessingException {
        PcqWithoutCaseResponse pcqWithoutCaseResponse = generateTestResponse("Invalid Request", 400);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(pcqWithoutCaseResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase()).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(400).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue( "Not the correct response", responseEntity.getBody() instanceof PcqWithoutCaseResponse);
        PcqWithoutCaseResponse responseBody = (PcqWithoutCaseResponse) responseEntity.getBody();
        assertEquals("PcqIds size don't match", 0, responseBody.getPcqId().length);
        assertEquals("Status code not correct", pcqWithoutCaseResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals("Status not correct", pcqWithoutCaseResponse.getResponseStatus(), responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase();

    }

    @Test
    public void testUnknownErrorResponse() throws JsonProcessingException {
        PcqWithoutCaseResponse pcqWithoutCaseResponse = generateTestResponse("Unknown error occurred", 500);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(pcqWithoutCaseResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase()).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(500).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue( "Not the correct response", responseEntity.getBody() instanceof PcqWithoutCaseResponse);
        PcqWithoutCaseResponse responseBody = (PcqWithoutCaseResponse) responseEntity.getBody();
        assertEquals("PcqIds size don't match", 0, responseBody.getPcqId().length);
        assertEquals("Status code not correct", pcqWithoutCaseResponse.getResponseStatusCode(),
                responseBody.getResponseStatusCode());
        assertEquals("Status not correct", pcqWithoutCaseResponse.getResponseStatus(), responseBody.getResponseStatus());


        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase();

    }

    @Test
    public void testOtherErrorResponse() throws JsonProcessingException {
        ErrorResponse errorResponse = generateErrorResponse();
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(errorResponse);

        when(mockPcqBackendFeignClient.getPcqWithoutCase()).thenReturn(Response.builder().request(mock(
                Request.class)).body(body, Charset.defaultCharset()).status(503).build());

        ResponseEntity responseEntity = pcqBackendService.getPcqWithoutCase();

        assertTrue( "Not the correct response", responseEntity.getBody() instanceof ErrorResponse);
        ErrorResponse responseBody = (ErrorResponse) responseEntity.getBody();
        assertEquals("Error message don't match", errorResponse.getErrorMessage(), responseBody.getErrorMessage());
        assertEquals("Description not correct", errorResponse.getErrorDescription(),
                responseBody.getErrorDescription());
        assertEquals("Timestamp not correct", errorResponse.getTimeStamp(), responseBody.getTimeStamp());

    }

    @Test
    public void executeFeignApiError() {
        ExternalApiException testException = new ExternalApiException(HttpStatus.BAD_GATEWAY, "Gateway Error");
        when(mockPcqBackendFeignClient.getPcqWithoutCase()).thenThrow(testException);

        assertThrows(ExternalApiException.class, () -> pcqBackendService.getPcqWithoutCase());

        verify(mockPcqBackendFeignClient, times(1)).getPcqWithoutCase();
    }


    private PcqWithoutCaseResponse generateTestResponse(String message, int statusCode) {
        PcqWithoutCaseResponse pcqWithoutCaseResponse = new PcqWithoutCaseResponse();
        pcqWithoutCaseResponse.setResponseStatus(message);
        pcqWithoutCaseResponse.setResponseStatusCode(String.valueOf(statusCode));

        if (statusCode == 200) {
            String[] pcqIds = {"PCQ_ID1", "PCQ_ID2"};
            pcqWithoutCaseResponse.setPcqId(pcqIds);
        }

        return pcqWithoutCaseResponse;
    }

    private ErrorResponse generateErrorResponse() {

        return new ErrorResponse("Bad Gateway",
                "Server did not respond", LocalDateTime.now().toString());
    }
}
