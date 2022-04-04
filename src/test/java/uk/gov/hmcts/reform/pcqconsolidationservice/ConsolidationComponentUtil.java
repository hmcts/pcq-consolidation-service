package uk.gov.hmcts.reform.pcqconsolidationservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcq.commons.model.SubmitResponse;

public final class ConsolidationComponentUtil {

    private ConsolidationComponentUtil() {
        //not called
    }

    public static ResponseEntity<SubmitResponse> generateSubmitTestSuccessResponse(
            String pcqId, String message, int statusCode) {
        SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setResponseStatus(message);
        submitResponse.setResponseStatusCode(String.valueOf(statusCode));
        submitResponse.setPcqId(pcqId);

        return new ResponseEntity<SubmitResponse>(submitResponse, HttpStatus.valueOf(statusCode));
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static PcqAnswerResponse generateTestAnswer(String pcqId, String serviceId, String actor, String dcn) {
        PcqAnswerResponse answerResponse = new PcqAnswerResponse();
        answerResponse.setPcqId(pcqId);
        answerResponse.setServiceId(serviceId);
        answerResponse.setActor(actor);
        answerResponse.setDcnNumber(dcn);

        return answerResponse;
    }
}
