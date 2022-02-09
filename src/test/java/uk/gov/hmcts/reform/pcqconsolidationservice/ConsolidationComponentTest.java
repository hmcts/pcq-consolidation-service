package uk.gov.hmcts.reform.pcqconsolidationservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcq.commons.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcq.commons.model.PcqRecordWithoutCaseResponse;
import uk.gov.hmcts.reform.pcq.commons.model.SubmitResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.CaseFieldMapping;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigHelper;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigItem;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigProvider;
import uk.gov.hmcts.reform.pcqconsolidationservice.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.pcqconsolidationservice.service.PcqBackendService;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd.CcdClientApi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.TooManyMethods")
@ExtendWith(MockitoExtension.class)
class ConsolidationComponentTest {

    @InjectMocks
    private ConsolidationComponent testConsolidationComponent;

    @Mock
    private PcqBackendService pcqBackendService;

    @Mock
    private ServiceConfigProvider serviceConfigProvider;

    @Mock
    private CcdClientApi ccdClientApi;

    @Mock
    private ResponseEntity<PcqRecordWithoutCaseResponse> pcqRecordWithoutCaseResponseResponseEntity;

    @Mock
    private PcqRecordWithoutCaseResponse pcqRecordWithoutCaseResponse;

    @Mock
    private ResponseEntity<SubmitResponse> submitResponseResponseEntity;

    private Set<String> serviceSet;

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
    private static final String DOCUMENT_DCN = "document.fileName";
    private static final String DOCUMENT_PREFIX_DCN = ".pdf";
    private static final String FIELD_DCN_1 = "12345";
    private static final String FIELD_DCN_2 = null;


    private static final PcqAnswerResponse[] EMPTY_PCQ_ANSWER_RESPONSE = {};

    private static final CaseFieldMapping CASE_FIELD_MAPPING_1
            = ServiceConfigHelper.createCaseFieldMap(ACTOR_NAME_1, FIELD_NAME_1);
    private static final CaseFieldMapping CASE_FIELD_MAPPING_2
            = ServiceConfigHelper.createCaseFieldMap(ACTOR_NAME_2, FIELD_NAME_2);

    private static final ServiceConfigItem SERVICE_CONFIG =
            ServiceConfigHelper.serviceConfigItem(
                    SERVICE_NAME_1,
                    Arrays.asList(CASE_TYPE_ID),
                    Arrays.asList(CASE_FIELD_MAPPING_1, CASE_FIELD_MAPPING_2),
                    DOCUMENT_DCN,
                    DOCUMENT_PREFIX_DCN
            );

    @Test
    void executeApiSuccess() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_1, SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_2, SUCCESS, 200));
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByOriginatingFormDcn(anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));

        when(serviceConfigProvider.getServiceNames()).thenReturn(getServiceSet());

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_2);
        verify(serviceConfigProvider, times(1)).getServiceNames();
        verify(ccdClientApi, times(1)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    void executeApiNoServiceConfigForPcqSearch() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(serviceConfigProvider.getConfig(SERVICE_NAME_1))
                .thenThrow(new ServiceNotConfiguredException("Config not found."));
        when(serviceConfigProvider.getConfig(SERVICE_NAME_2)).thenReturn(SERVICE_CONFIG);

        testConsolidationComponent.execute();

        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_2);
        verify(pcqBackendService, times(0)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(ccdClientApi, times(0)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    void executeApiNoServiceConfigForDnsSearch() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(serviceConfigProvider.getConfig(SERVICE_NAME_1)).thenReturn(SERVICE_CONFIG);
        when(serviceConfigProvider.getConfig(SERVICE_NAME_2))
                .thenThrow(new ServiceNotConfiguredException("Config not found."));

        testConsolidationComponent.execute();

        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_2);
        verify(pcqBackendService, times(0)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(ccdClientApi, times(1)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
        verify(ccdClientApi, times(0)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    void executeApiNoCaseMatchesFoundInSearch() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList());
        when(ccdClientApi.getCaseRefsByOriginatingFormDcn(anyString(), anyString()))
                .thenReturn(Arrays.asList());

        when(serviceConfigProvider.getServiceNames()).thenReturn(getServiceSet());

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(0)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(0)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_2);
        verify(serviceConfigProvider, times(1)).getServiceNames();
        verify(ccdClientApi, times(1)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    void executeApiError() {
        ExternalApiException testException = new ExternalApiException(HttpStatus.BAD_GATEWAY, "Gateway Error");
        when(pcqBackendService.getPcqWithoutCase()).thenThrow(testException);

        assertThrows(ExternalApiException.class, () -> testConsolidationComponent.execute());

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
    }

    @Test
    void executeApiErrorAddCase() {
        ExternalApiException testException = new ExternalApiException(HttpStatus.BAD_GATEWAY,
                "Add Case Gateway Error");
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenThrow(testException);
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByOriginatingFormDcn(anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        when(serviceConfigProvider.getServiceNames()).thenReturn(getServiceSet());

        assertThrows(ExternalApiException.class, () -> testConsolidationComponent.execute());

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getServiceNames();
        verify(ccdClientApi, times(1)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
    }

    @Test
    void executeApiInvalidRequest() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse("Invalid Request", 400));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
    }

    @Test
    void executeApiNullBodyErrorFromBackendService() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(pcqRecordWithoutCaseResponseResponseEntity);
        when(pcqRecordWithoutCaseResponseResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(pcqRecordWithoutCaseResponseResponseEntity.getBody()).thenReturn(null);

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqRecordWithoutCaseResponseResponseEntity, times(1)).getBody();
    }

    @Test
    void executeApiPcqWithoutCaseResponseIsNullError() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(pcqRecordWithoutCaseResponseResponseEntity);
        when(pcqRecordWithoutCaseResponseResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(pcqRecordWithoutCaseResponseResponseEntity.getBody()).thenReturn(pcqRecordWithoutCaseResponse);
        when(pcqRecordWithoutCaseResponse.getPcqRecord()).thenReturn(EMPTY_PCQ_ANSWER_RESPONSE);

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqRecordWithoutCaseResponseResponseEntity, times(1)).getBody();
        verify(pcqRecordWithoutCaseResponse, times(1)).getPcqRecord();
    }

    @Test
    void executeApiInvalidRequestAddCase() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_1, SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_2, "Invalid Request", 400));
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByOriginatingFormDcn(anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        when(serviceConfigProvider.getServiceNames()).thenReturn(getServiceSet());

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getServiceNames();
        verify(ccdClientApi, times(1)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    void executeApiEmptyBodyErrorFromAddCase() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenReturn(
                submitResponseResponseEntity);
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString())).thenReturn(
                submitResponseResponseEntity);
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByOriginatingFormDcn(anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        when(submitResponseResponseEntity.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(submitResponseResponseEntity.getBody()).thenReturn(null);
        when(serviceConfigProvider.getServiceNames()).thenReturn(getServiceSet());

        testConsolidationComponent.execute();

        verify(submitResponseResponseEntity, times(2)).getBody();
        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getServiceNames();
        verify(ccdClientApi, times(1)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @Test
    void executeApiInternalError() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse("Unknown error", 500));

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
    }

    @Test
    void executeApiInternalErrorAddCase() {
        when(pcqBackendService.getPcqWithoutCase()).thenReturn(generateTestSuccessResponse(SUCCESS, 200));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_1, "Unknown error", 500));
        when(pcqBackendService.addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString())).thenReturn(
                ConsolidationComponentUtil.generateSubmitTestSuccessResponse(TEST_PCQ_ID_2, SUCCESS, 200));
        when(serviceConfigProvider.getConfig(anyString())).thenReturn(SERVICE_CONFIG);
        when(ccdClientApi.getCaseRefsByOriginatingFormDcn(anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));
        when(ccdClientApi.getCaseRefsByPcqId(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(TEST_CASE_ID));

        when(serviceConfigProvider.getServiceNames()).thenReturn(getServiceSet());

        testConsolidationComponent.execute();

        verify(pcqBackendService, times(1)).getPcqWithoutCase();
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_1, TEST_CASE_ID.toString());
        verify(pcqBackendService, times(1)).addCaseForPcq(TEST_PCQ_ID_2, TEST_CASE_ID.toString());
        verify(serviceConfigProvider, times(1)).getConfig(SERVICE_NAME_1);
        verify(serviceConfigProvider, times(1)).getServiceNames();
        verify(ccdClientApi, times(1)).getCaseRefsByOriginatingFormDcn(FIELD_DCN_1, SERVICE_NAME_1);
        verify(ccdClientApi, times(1)).getCaseRefsByPcqId(TEST_PCQ_ID_2, SERVICE_NAME_1, ACTOR_NAME_2);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity generateTestSuccessResponse(String message, int statusCode) {
        PcqRecordWithoutCaseResponse pcqWithoutCaseResponse = new PcqRecordWithoutCaseResponse();
        pcqWithoutCaseResponse.setResponseStatus(message);
        pcqWithoutCaseResponse.setResponseStatusCode(String.valueOf(statusCode));

        PcqAnswerResponse answerResponse1
                = ConsolidationComponentUtil.generateTestAnswer(
                        TEST_PCQ_ID_1, SERVICE_NAME_1, ACTOR_NAME_1, FIELD_DCN_1);
        PcqAnswerResponse answerResponse2
                = ConsolidationComponentUtil.generateTestAnswer(
                        TEST_PCQ_ID_2, SERVICE_NAME_2, ACTOR_NAME_2, FIELD_DCN_2);

        PcqAnswerResponse[] answerResponses = {answerResponse1, answerResponse2};
        pcqWithoutCaseResponse.setPcqRecord(answerResponses);

        return new ResponseEntity(pcqWithoutCaseResponse, HttpStatus.valueOf(statusCode));
    }

    private Set<String> getServiceSet() {
        if (serviceSet == null) {
            serviceSet = new HashSet<>();
            serviceSet.add(SERVICE_NAME_1);
            serviceSet.add(SERVICE_NAME_2);
        }

        return serviceSet;
    }

}