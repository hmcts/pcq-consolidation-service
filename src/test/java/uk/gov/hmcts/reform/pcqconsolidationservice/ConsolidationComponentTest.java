package uk.gov.hmcts.reform.pcqconsolidationservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.CaseFieldMapping;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigHelper;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigItem;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigProvider;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.response.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.controller.response.PcqRecordWithoutCaseResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcqconsolidationservice.service.PcqBackendService;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd.CcdClientApi;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.TooManyMethods")
@ExtendWith(MockitoExtension.class)
public class ConsolidationComponentTest {

    @InjectMocks
    private ConsolidationComponent testConsolidationComponent;

    @Mock
    private PcqBackendService pcqBackendService;

    @Mock
    private ServiceConfigProvider serviceConfigProvider;

    @Mock
    private CcdClientApi ccdClientApi;

    private static final String TEST_PCQ_ID_1 = "PCQ_ID1";
    private static final String TEST_PCQ_ID_2 = "PCQ_ID2";
    private static final Long TEST_CASE_ID = 484_757_637_549L;
    private static final String SUCCESS = "Success";
    private static final String SERVICE_NAME_1 = "SERVICE_JD1";
    private static final String SERVICE_NAME_2 = "SERVICE_JD2";
    private static final String CASE_TYPE_ID = "CaseTypeA";
    private static final String ACTOR_NAME_1 = "ACTOR_1";
    private static final String ACTOR_NAME_2 = "ACTOR_2";
    private static final String FIELD_NAME_1 = "pcqId1";
    private static final String FIELD_NAME_2 = "pcqId2";

    private static final CaseFieldMapping CASE_FIELD_MAPPING_1
            = ServiceConfigHelper.createCaseFieldMap(ACTOR_NAME_1, FIELD_NAME_1);
    private static final CaseFieldMapping CASE_FIELD_MAPPING_2
            = ServiceConfigHelper.createCaseFieldMap(ACTOR_NAME_2, FIELD_NAME_2);

    private static final ServiceConfigItem SERVICE_CONFIG =
            ServiceConfigHelper.serviceConfigItem(
                    SERVICE_NAME_1,
                    Arrays.asList(CASE_TYPE_ID),
                    Arrays.asList(CASE_FIELD_MAPPING_1, CASE_FIELD_MAPPING_2));

    @Test
    public void executeApiSuccess() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_1, SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_2, SUCCESS, 200));
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_2);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_1, SERVICE_NAME_1, ACTOR_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    public void executeApiError() {
        ExternalApiException testException = new ExternalApiException(HttpStatus.BAD_GATEWAY, "Gateway Error");
        when(pcqBackendService.getPcqWithoutCase()).thenThrow(testException);

        assertThrows(ExternalApiException.class, () -> testConsolidationComponent.execute());

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
    }

    @Test
    public void executeApiErrorAddCase() {
        ExternalApiException testException = new ExternalApiException(HttpStatus.BAD_GATEWAY,
                "Add Case Gateway Error");
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenThrow(testException);
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        assertThrows(ExternalApiException.class, () -> testConsolidationComponent.execute());

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_1, SERVICE_NAME_1, ACTOR_NAME_1);
    }

    @Test
    public void executeApiInvalidRequest() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse("Invalid Request", 400));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
    }

    @Test
    public void executeApiServiceUnavailable() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse("Service Unavailable", 503));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
    }

    @Test
    public void executeApiInvalidRequestAddCase() {
        this.apiAddCaseError(400, "Invalid Request");
    }

    @Test
    public void executeApiServiceUnavailableAddCase() {
        this.apiAddCaseError(502, "Service Unavailable");
    }

    public void apiAddCaseError(int statusCode, String error) {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_1, SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_2, error, statusCode));
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_1, SERVICE_NAME_1, ACTOR_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    public void executeApiInternalError() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse("Unknown error", 500));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
    }

    @Test
    public void executeApiInternalErrorAddCase() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_1, "Unknown error", 500));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_2, SUCCESS, 200));
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_1, SERVICE_NAME_1, ACTOR_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity generateTestSuccessResponse(String message, int statusCode) {
        PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = new PcqRecordWithoutCaseResponse();
        pcqWithoutCaseResponse.setResponseStatus(message);
        pcqWithoutCaseResponse.setResponseStatusCode(String.valueOf(statusCode));

        PcqAnswerResponse answerResponse1
                = ConsolidationComponentUtil.generateTestAnswer(TEST_PCQ_ID_1, SERVICE_NAME_1, ACTOR_NAME_1);
        PcqAnswerResponse answerResponse2
                = ConsolidationComponentUtil.generateTestAnswer(TEST_PCQ_ID_2, SERVICE_NAME_2, ACTOR_NAME_2);

        PcqAnswerResponse[] answerResponses = {answerResponse1, answerResponse2};
        pcqWithoutCaseResponse.setPcqRecord(answerResponses);

        return new ResponseEntity(pcqWithoutCaseResponse, HttpStatus.valueOf(statusCode));
    }

}