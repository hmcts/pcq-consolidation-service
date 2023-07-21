package uk.gov.hmcts.reform.pcqconsolidationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcq.commons.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcq.commons.model.PcqRecordWithoutCaseResponse;
import uk.gov.hmcts.reform.pcq.commons.model.SubmitResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigItem;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigProvider;
import uk.gov.hmcts.reform.pcqconsolidationservice.exception.ServiceFeignException;
import uk.gov.hmcts.reform.pcqconsolidationservice.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.pcqconsolidationservice.service.PcqBackendService;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd.CcdClientApi;
import uk.gov.hmcts.reform.pcqconsolidationservice.utils.LoggingSummaryUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ConsolidationComponent {

    private final Map<String, PcqAnswerResponse[]> pcqIdsMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> serviceSummaryMap = new ConcurrentHashMap<>();

    private static final String ONLINE_MATCH_SUFFIX = "_online_channel_matched";
    private static final String ONLINE_NOT_FOUND_SUFFIX = "_online_channel_not_found";
    private static final String ONLINE_ERROR_SUFFIX = "_online_channel_error";
    private static final String PAPER_MATCH_SUFFIX = "_paper_channel_matched";
    private static final String PAPER_NOT_FOUND_SUFFIX = "_paper_channel_not_found";
    private static final String PAPER_ERROR_SUFFIX = "_paper_channel_error";

    @Autowired
    private CcdClientApi ccdClientApi;

    @Autowired
    private PcqBackendService pcqBackendService;

    @Autowired
    private ServiceConfigProvider serviceConfigProvider;

    @SuppressWarnings({"unchecked", "PMD.DataflowAnomalyAnalysis"})
    public void execute() {
        try {
            // Step 1. Get the list of PCQs without Case Id.
            ResponseEntity<PcqRecordWithoutCaseResponse> responseEntity = pcqBackendService.getPcqWithoutCase();
            PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = responseEntity.getBody();
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                processPcqRecordsWithoutCase(pcqWithoutCaseResponse);

            } else {
                if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST || responseEntity.getStatusCode()
                        == HttpStatus.INTERNAL_SERVER_ERROR) {
                    if (pcqWithoutCaseResponse == null || pcqWithoutCaseResponse.getResponseStatus() == null) {
                        log.error("Response from backend service invalid, missing body");
                    } else {
                        log.error("PcqWithoutCase API generated error message {} ",
                                pcqWithoutCaseResponse.getResponseStatus());
                    }
                } else {
                    log.error("PcqWithoutCase API generated unknown error message");
                }
            }

        } catch (ExternalApiException externalApiException) {
            log.error("API could not be invoked due to error message - {}", externalApiException.getErrorMessage());
            throw externalApiException;
        } finally {
            LoggingSummaryUtils.logSummary(serviceSummaryMap, serviceConfigProvider.getServiceNames());
        }
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis","PMD.ConfusingTernary"})
    private void processPcqRecordsWithoutCase(PcqRecordWithoutCaseResponse pcqWithoutCaseResponse) {
        if (pcqWithoutCaseResponse == null || pcqWithoutCaseResponse.getPcqRecord().length == 0) {
            log.info("Pcq Ids, without case information, are not found");

        } else {
            pcqIdsMap.put("PCQ_ID_FOUND", pcqWithoutCaseResponse.getPcqRecord());
            for (PcqAnswerResponse pcqAnswerResponse : pcqWithoutCaseResponse.getPcqRecord()) {
                Long caseReference = null;

                //Step 2, Check for DCN, if available invoke Elastic Search API to get the case Id.
                if (pcqAnswerResponse.getDcnNumber() != null && !pcqAnswerResponse.getDcnNumber().isEmpty()) {
                    caseReference = findCaseReferenceFromDcn(
                            pcqAnswerResponse.getDcnNumber(),
                            pcqAnswerResponse.getServiceId());

                } else if (pcqAnswerResponse.getPcqId() != null && !pcqAnswerResponse.getPcqId().isEmpty()) {
                    //Step 3, No DCN so invoke Elastic Search API on pcqId to get the case Id.
                    caseReference = findCaseReferenceFromPcqId(
                            pcqAnswerResponse.getPcqId(),
                            pcqAnswerResponse.getServiceId(),
                            pcqAnswerResponse.getActor());
                }

                if (caseReference != null) {
                    //Step 4, Invoke the addCaseForPcq API to update the case id for the Pcq.
                    invokeAddCaseForPcq(pcqAnswerResponse.getPcqId(), caseReference.toString());
                }
            }
            pcqIdsMap.put("PCQ_ID_PROCESSED", pcqWithoutCaseResponse.getPcqRecord());
        }
    }

    private Long findCaseReferenceFromPcqId(String pcqId, String serviceId, String actor) {
        try {
            ServiceConfigItem serviceConfigItemByServiceId = serviceConfigProvider.getConfig(serviceId);
            List<Long> caseReferences
                    = ccdClientApi.getCaseRefsByPcqId(pcqId, serviceConfigItemByServiceId.getService(), actor);

            if (caseReferences != null && caseReferences.size() == 1) {
                Long caseReferenceForPcq = caseReferences.get(0);
                log.info("Found {} case reference {} for PCQ ID {}", serviceId, caseReferenceForPcq, pcqId);
                incrementServiceCount(serviceId + ONLINE_MATCH_SUFFIX);
                return caseReferenceForPcq;
            } else {
                log.info("Unable to find {} case reference for PCQ ID {}", serviceId, pcqId);
                incrementServiceCount(serviceId + ONLINE_NOT_FOUND_SUFFIX);
            }

        } catch (ServiceNotConfiguredException snce) {
            log.error("Error searching cases for PCQ ID {} as no {} configuration was found", pcqId, serviceId);
            incrementServiceCount(serviceId + ONLINE_ERROR_SUFFIX);
        } catch (ServiceFeignException e) {
            ccdClientApi.refreshToken();
            log.error("Error searching cases for PCQ ID {} as FeignException was thrown : {} ", pcqId, e);
            incrementServiceCount(serviceId + ONLINE_ERROR_SUFFIX);
        } catch (Exception e) {
            log.error("Error searching cases for PCQ ID {} as Exception was thrown : {}", pcqId, e);
            incrementServiceCount(serviceId + ONLINE_ERROR_SUFFIX);
        }

        return null;
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    private Long findCaseReferenceFromDcn(String dcn, String serviceId) {
        Long caseReferenceForPcq = null;

        try {
            ServiceConfigItem serviceConfigItemByServiceId = serviceConfigProvider.getConfig(serviceId);
            List<Long> caseReferences
                    = ccdClientApi.getCaseRefsByOriginatingFormDcn(dcn, serviceConfigItemByServiceId.getService());

            if (caseReferences != null && caseReferences.size() == 1) {
                caseReferenceForPcq = caseReferences.get(0);
                log.info("Found {} case reference {} for DCN {}", serviceId, caseReferenceForPcq, dcn);
                incrementServiceCount(serviceId + PAPER_MATCH_SUFFIX);
            } else {
                log.info("Unable to find {} case reference for DCN {}", serviceId, dcn);
                incrementServiceCount(serviceId + PAPER_NOT_FOUND_SUFFIX);
            }

        } catch (ServiceNotConfiguredException snce) {
            log.error("Error searching cases for DCN {} as no {} configuration was found", dcn, serviceId);
            incrementServiceCount(serviceId + PAPER_ERROR_SUFFIX);
        } catch (ServiceFeignException e) {
            ccdClientApi.refreshToken();
            log.error("Error searching cases for PCQ ID {} as FeignException was thrown : {} ", dcn, e);
            incrementServiceCount(serviceId + ONLINE_ERROR_SUFFIX);
        } catch (Exception e) {
            log.error("Error searching cases for PCQ ID {} as Exception was thrown : {}", dcn, e);
            incrementServiceCount(serviceId + ONLINE_ERROR_SUFFIX);
        }

        return caseReferenceForPcq;
    }

    @SuppressWarnings({"unchecked"})
    private void invokeAddCaseForPcq(String pcqId, String caseId) {
        ResponseEntity<SubmitResponse> responseEntity = pcqBackendService.addCaseForPcq(pcqId, caseId);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully added case {} for PCQ ID {}", caseId, pcqId);

        } else {
            if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST || responseEntity.getStatusCode()
                    == HttpStatus.INTERNAL_SERVER_ERROR) {
                SubmitResponse submitResponse = responseEntity.getBody();
                if (submitResponse == null || submitResponse.getResponseStatus() == null) {
                    log.error("Response from backend service invalid, missing body");
                } else {
                    log.error("AddCaseForPcq API generated error message {}", submitResponse.getResponseStatus());
                }
            } else {
                log.error("AddCaseForPcq API generated unknown error message");
            }
        }
    }

    private void incrementServiceCount(String service) {

        String serviceKey = service.toLowerCase(Locale.UK);

        if (serviceSummaryMap.get(serviceKey) == null) {
            serviceSummaryMap.put(serviceKey, 1);
        } else {
            int count = serviceSummaryMap.get(serviceKey) + 1;
            serviceSummaryMap.put(serviceKey, count);
        }
    }

    public Map<String, PcqAnswerResponse[]> getPcqIdsMap() {
        return pcqIdsMap;
    }

}
