package uk.gov.hmcts.reform.pcqconsolidationservice.controllers.advice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.advice.ErrorResponse;


class ErrorResponseTest {

    @Test
    void testErrorResponse() {
        String expectMsg = "msg";

        ErrorResponse errorDetails = ErrorResponse.builder()
                .errorDescription("desc")
                .errorMessage(expectMsg)
                .timeStamp("time")
                .build();

        final String expectDesc = "desc";
        final String expectTs = "time";
        Assertions.assertNotNull(errorDetails, "Error Response is null");
        Assertions.assertEquals(expectMsg, errorDetails.getErrorMessage(), "Error message is not expected");
        Assertions.assertEquals(expectTs, errorDetails.getTimeStamp(), "Timestamp is not correct");
        Assertions.assertEquals(expectDesc, errorDetails.getErrorDescription(), "Error description is not correct");
    }

    @Test
    void testNoArgsConstructor() {
        ErrorResponse errorResponse = new ErrorResponse();
        Assertions.assertNotNull(errorResponse, "ErrorResponse is null");
    }

}
