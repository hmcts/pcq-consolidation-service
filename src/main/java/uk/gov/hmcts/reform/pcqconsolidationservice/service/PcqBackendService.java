package uk.gov.hmcts.reform.pcqconsolidationservice.service;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcq.commons.model.PcqRecordWithoutCaseResponse;
import uk.gov.hmcts.reform.pcq.commons.model.SubmitResponse;

public interface PcqBackendService {

    ResponseEntity<PcqRecordWithoutCaseResponse> getPcqWithoutCase();

    ResponseEntity<SubmitResponse> addCaseForPcq(String pcqId, String caseId);

}
