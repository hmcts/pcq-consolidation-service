package uk.gov.hmcts.reform.pcqconsolidationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.pcqconsolidationservice.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.pcqconsolidationservice.util.SpringBootIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(locations = "/application.properties")
class ServiceConfigTest extends SpringBootIntegrationTest {

    private static final String TEST_SERVICE_UNKNOWN_NAME = "madeupname";

    // Example Service 1 - Probate
    private static final String TEST_SERVICE1_NAME = "PROBATE";
    private static final String TEST_SERVICE1_CASE_TYPE_ID_1 = "Caveat";
    private static final String TEST_SERVICE1_CASE_TYPE_ID_2 = "GrantOfRepresentation";
    private static final String TEST_SERVICE1_CASE_FIELD_MAP_ACTOR_1 = "APPLICANT";
    private static final String TEST_SERVICE1_CASE_FIELD_MAP_NAME_1 = "pcqId";
    private static final int TEST_SERVICE1_CASE_TYPES_SIZE = 2;

    // Example Service 2 - Divorce
    private static final String TEST_SERVICE2_NAME = "DIVORCE";
    private static final String TEST_SERVICE2_CASE_TYPE_ID_1 = "DIVORCE";
    private static final String TEST_SERVICE2_CASE_FIELD_MAP_ACTOR_1 = "PETITIONER";
    private static final String TEST_SERVICE2_CASE_FIELD_MAP_NAME_1 = "PetitionerPcqId";
    private static final String TEST_SERVICE2_CASE_FIELD_MAP_ACTOR_2 = "RESPONDENT";
    private static final String TEST_SERVICE2_CASE_FIELD_MAP_NAME_2 = "RespondentPcqId";
    private static final String TEST_SERVICE2_CASE_FIELD_MAP_ACTOR_3 = "CORESPONDENT";
    private static final String TEST_SERVICE2_CASE_FIELD_MAP_NAME_3 = "CoRespondentPcqId";
    private static final int TEST_SERVICE2_CASE_TYPES_SIZE = 1;

    // Example Service 3 - CMC
    private static final String TEST_SERVICE3_NAME = "CMC";
    private static final String TEST_SERVICE3_CASE_TYPE_ID_1 = "MoneyClaimCase";
    private static final String TEST_SERVICE3_CASE_FIELD_MAP_ACTOR_1 = "CLAIMANT";
    private static final String TEST_SERVICE3_CASE_FIELD_MAP_NAME_1 = "applicants.value.pcqId";
    private static final String TEST_SERVICE3_CASE_FIELD_MAP_ACTOR_2 = "DEFENDANT";
    private static final String TEST_SERVICE3_CASE_FIELD_MAP_NAME_2 = "respondents.value.pcqId";
    private static final int TEST_SERVICE3_CASE_TYPES_SIZE = 1;

    // Example Service 4 - SSCS
    private static final String TEST_SERVICE4_NAME = "SSCS";
    private static final String TEST_SERVICE4_CASE_TYPE_ID_1 = "Benefit";
    private static final String TEST_SERVICE4_CASE_FIELD_MAP_ACTOR_1 = "APPELLANT";
    private static final String TEST_SERVICE4_CASE_FIELD_MAP_NAME_1 = "pcqId";
    private static final String TEST_SERVICE4_CASE_DCN_MAPPING = "sscsDocument.value.documentFileName";
    private static final String TEST_SERVICE4_CASE_DCN_SUFFIX = ".pdf";
    private static final int TEST_SERVICE4_CASE_TYPES_SIZE = 1;

    // Messages
    private static final String TEST_SERVICE_NAME_MESSAGE = "Case Service Name is correct";
    private static final String TEST_CASE_TYPE_MESSAGE = "Case Type is correct";
    private static final String TEST_ACTOR_COMPARISON_MESSAGE = "Case Actor Mapping is correct";
    private static final String TEST_NO_CASE_TYPES_MESSAGE = "Number of Case Types is correct";
    private static final String TEST_DCN_DOCUMENT_MAPPING_MESSAGE = "Case Document DCN Mapping is correct";
    private static final String TEST_DCN_DOCUMENT_SUFFIX_MESSAGE = "Case Document DCN Suffix is correct";

    @Autowired
    private ServiceConfigProvider serviceConfigProvider;

    @Test
    public void serviceConfigItemShouldCaptureServiceWithMultipleCaseTypes() {
        ServiceConfigItem configItem = serviceConfigProvider.getConfig(TEST_SERVICE1_NAME);

        assertEquals(TEST_SERVICE1_NAME,
                configItem.getService(),TEST_SERVICE_NAME_MESSAGE);
        assertEquals(TEST_SERVICE1_CASE_TYPES_SIZE,
                configItem.getCaseTypeIds().size(), TEST_NO_CASE_TYPES_MESSAGE);
        assertEquals(TEST_SERVICE1_CASE_TYPE_ID_1,
                configItem.getCaseTypeIds().get(0), TEST_CASE_TYPE_MESSAGE);
        assertEquals(TEST_SERVICE1_CASE_TYPE_ID_2,
                configItem.getCaseTypeIds().get(1), TEST_CASE_TYPE_MESSAGE);
        assertEquals(TEST_SERVICE1_CASE_FIELD_MAP_NAME_1,
                configItem.getCaseField(TEST_SERVICE1_CASE_FIELD_MAP_ACTOR_1), TEST_ACTOR_COMPARISON_MESSAGE);
    }

    @Test
    public void serviceConfigItemShouldCaptureServiceWithMultipleActors() {
        ServiceConfigItem configItem = serviceConfigProvider.getConfig(TEST_SERVICE2_NAME);

        assertEquals(TEST_SERVICE2_NAME,
                configItem.getService(),TEST_SERVICE_NAME_MESSAGE);
        assertEquals(TEST_SERVICE2_CASE_TYPES_SIZE,
                configItem.getCaseTypeIds().size(), TEST_NO_CASE_TYPES_MESSAGE);
        assertEquals(TEST_SERVICE2_CASE_TYPE_ID_1,
                configItem.getCaseTypeIds().get(0), TEST_CASE_TYPE_MESSAGE);
        assertEquals(TEST_SERVICE2_CASE_FIELD_MAP_NAME_1,
                configItem.getCaseField(TEST_SERVICE2_CASE_FIELD_MAP_ACTOR_1), TEST_ACTOR_COMPARISON_MESSAGE);
        assertEquals(TEST_SERVICE2_CASE_FIELD_MAP_NAME_2,
                configItem.getCaseField(TEST_SERVICE2_CASE_FIELD_MAP_ACTOR_2), TEST_ACTOR_COMPARISON_MESSAGE);
        assertEquals(TEST_SERVICE2_CASE_FIELD_MAP_NAME_3,
                configItem.getCaseField(TEST_SERVICE2_CASE_FIELD_MAP_ACTOR_3), TEST_ACTOR_COMPARISON_MESSAGE);
    }

    @Test
    public void serviceConfigItemShouldCaptureServiceWithNestedPcqNames() {
        ServiceConfigItem configItem = serviceConfigProvider.getConfig(TEST_SERVICE3_NAME);

        assertEquals(TEST_SERVICE3_NAME,
                configItem.getService(),TEST_SERVICE_NAME_MESSAGE);
        assertEquals(TEST_SERVICE3_CASE_TYPES_SIZE,
                configItem.getCaseTypeIds().size(), TEST_NO_CASE_TYPES_MESSAGE);
        assertEquals(TEST_SERVICE3_CASE_TYPE_ID_1,
                configItem.getCaseTypeIds().get(0), TEST_CASE_TYPE_MESSAGE);
        assertEquals(TEST_SERVICE3_CASE_FIELD_MAP_NAME_1,
                configItem.getCaseField(TEST_SERVICE3_CASE_FIELD_MAP_ACTOR_1), TEST_ACTOR_COMPARISON_MESSAGE);
        assertEquals(TEST_SERVICE3_CASE_FIELD_MAP_NAME_2,
                configItem.getCaseField(TEST_SERVICE3_CASE_FIELD_MAP_ACTOR_2), TEST_ACTOR_COMPARISON_MESSAGE);
    }

    @Test
    public void serviceConfigItemShouldCaptureServiceCustomDcnMapping() {
        ServiceConfigItem configItem = serviceConfigProvider.getConfig(TEST_SERVICE4_NAME);

        assertEquals(TEST_SERVICE4_NAME,
                configItem.getService(),TEST_SERVICE_NAME_MESSAGE);
        assertEquals(TEST_SERVICE4_CASE_TYPES_SIZE,
                configItem.getCaseTypeIds().size(), TEST_NO_CASE_TYPES_MESSAGE);
        assertEquals(TEST_SERVICE4_CASE_TYPE_ID_1,
                configItem.getCaseTypeIds().get(0), TEST_CASE_TYPE_MESSAGE);
        assertEquals(TEST_SERVICE4_CASE_FIELD_MAP_NAME_1,
                configItem.getCaseField(TEST_SERVICE4_CASE_FIELD_MAP_ACTOR_1), TEST_ACTOR_COMPARISON_MESSAGE);
        assertEquals(TEST_SERVICE4_CASE_DCN_MAPPING,
                configItem.getCaseDcnDocumentMapping(), TEST_DCN_DOCUMENT_MAPPING_MESSAGE);
        assertEquals(TEST_SERVICE4_CASE_DCN_SUFFIX,
                configItem.getCaseDcnDocumentSuffix(), TEST_DCN_DOCUMENT_SUFFIX_MESSAGE);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    @Test
    public void serviceConfigItemShouldThrowExeceptionIfMissing() {
        Exception exception = assertThrows(ServiceNotConfiguredException.class, () -> {
            serviceConfigProvider.getConfig(TEST_SERVICE_UNKNOWN_NAME);
        });

        String expectedMessage = "Service madeupname is not configured";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage), "Expected error message for service not found");
    }
}
