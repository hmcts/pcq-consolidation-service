package uk.gov.hmcts.reform.pcqconsolidationservice.postdeploy;

import com.gilecode.reflection.ReflectionAccessUtils;
import com.gilecode.reflection.ReflectionAccessor;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.ConsolidationComponent;
import uk.gov.hmcts.reform.pcqconsolidationservice.ccd.model.PcqQuestions;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.TestApplicationConfiguration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplicationConfiguration.class)
@ActiveProfiles("functional")
@Slf4j
public class ConsolidationServiceFunctionalTest extends ConsolidationServiceTestBase {

    public static final String TEST_DIGITAL_CASE_TITLE = "Func-Test-Digital-Case-6";
    public static final String TEST_PAPER_CASE_TITLE = "Func-Test-Paper-Case-6";

    public static final String TEST_PCQ_ID_2 = "73c5a4de-932a-2093-851a-da3b99a70bba";
    public static final String TEST_PCQ_ID_3 = "b0ab25a9-a04d-2ba1-b9f5-3108b7b7884c";

    @Value("${pcqBackendUrl}")
    private String pcqBackendUrl;

    @Value("${jwt_test_secret}")
    private String jwtSecretKey;

    @Autowired
    private ConsolidationComponent consolidationComponent;

    private CaseDetails pcqCase1;
    private CaseDetails pcqCase2;
    private String testPcqId1;

    @Before
    public void createPcqData() throws IOException {
        // Create the Sample service core case data.

        pcqCase1 = createCcdPcqQuestionsDigitalCase(TEST_DIGITAL_CASE_TITLE);
        pcqCase2 = createCcdPcqQuestionsPaperCase(TEST_PAPER_CASE_TITLE);

        // Create the PCQ answer records.
        testPcqId1  = ((PcqQuestions)((CaseDataContent)pcqCase1.getData()).getData()).getPcqId();
        createTestAnswerRecordWithoutCase(testPcqId1);
        createTestAnswerRecordDcnWithoutCase(TEST_PCQ_ID_2);
        createTestAnswerRecordWithCase(TEST_PCQ_ID_3);


    }

    @After
    public void removePcqData() throws IOException {

        // Remove the PCQ answer records.
        removeTestAnswerRecord(testPcqId1);
        removeTestAnswerRecord(TEST_PCQ_ID_2);
        removeTestAnswerRecord(TEST_PCQ_ID_3);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteMethod() throws IOException, IllegalAccessException {

        //Invoke the executor
        consolidationComponent.execute();

        //Make the status map accessible from the ConsolidationComponent.
        Field mapField = ReflectionUtils.findField(ConsolidationComponent.class, "pcqIdsMap");
        ReflectionAccessor accessor = ReflectionAccessUtils.getReflectionAccessor();
        accessor.makeAccessible(mapField);

        //Check that the API - pcqWithoutCase has been called and that the test records are found.
        Map<String, PcqAnswerResponse[]> statusMap = (Map<String, PcqAnswerResponse[]>)mapField.get(
                consolidationComponent);
        assertNotNull("Status Map is null", statusMap);
        PcqAnswerResponse[] pcqAnswerRecords = statusMap.get("PCQ_ID_FOUND");
        assertPcqIdsRetrieved(pcqAnswerRecords, testPcqId1, TEST_PCQ_ID_2, TEST_PCQ_ID_3);

        //Check that the API - addCaseForPcq has been called and that the test records are updated.
        PcqAnswerResponse[] pcqRecordsProcessed = statusMap.get("PCQ_ID_PROCESSED");
        assertPcqIdsProcessed(pcqRecordsProcessed, testPcqId1, TEST_PCQ_ID_2);

        //Make a call to the getAnswer from pcq backend to verify that case Id has been updated search by pcqId.
        PcqAnswerResponse answerResponse = getTestAnswerRecord(testPcqId1, pcqBackendUrl, jwtSecretKey);
        assertNotNull("The get response is not null", answerResponse);
        assertEquals("The get response matches ccd case id", pcqCase1.getId().toString(), answerResponse.getCaseId());

        //Make a call to the getAnswer from pcq backend to verify that case Id has been updated search by dcn.
        answerResponse = getTestAnswerRecord(TEST_PCQ_ID_2, pcqBackendUrl, jwtSecretKey);
        assertNotNull("The get response is not null", answerResponse);
        assertEquals("The get response matches ccd case id", pcqCase2.getId().toString(), answerResponse.getCaseId());
    }

    private void createTestAnswerRecordWithoutCase(String pcqId) throws IOException {
        createTestAnswerRecord("JsonTestFiles/FirstSubmitAnswer.json", pcqBackendUrl, pcqId, jwtSecretKey);
    }

    private void createTestAnswerRecordDcnWithoutCase(String pcqId) throws IOException {
        createTestAnswerRecord("JsonTestFiles/SecondSubmitAnswerWithDcn.json", pcqBackendUrl, pcqId, jwtSecretKey);
    }

    private void createTestAnswerRecordWithCase(String pcqId) throws IOException {
        createTestAnswerRecord("JsonTestFiles/FirstSubmitAnswerWithCase.json", pcqBackendUrl, pcqId, jwtSecretKey);
    }

    private void removeTestAnswerRecord(String pcqId) throws IOException {
        removeTestAnswerRecord(pcqBackendUrl, pcqId, jwtSecretKey);
    }

    protected void removeTestAnswerRecord(String apiUrl, String pcqId, String jwtSecretKey)
            throws IOException {
        deleteTestRecordFromBackend(apiUrl, pcqId, jwtSecretKey);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void assertPcqIdsRetrieved(PcqAnswerResponse[] pcqAnswerRecords,
                                       String pcqRecord1,String pcqRecord2,
                                       String pcqRecord3) {
        List<String> pcqIds = new ArrayList<>();
        for (PcqAnswerResponse answerResponse : pcqAnswerRecords) {
            pcqIds.add(answerResponse.getPcqId());
        }

        assertTrue("The pcqRecord 1 is not found.", pcqIds.contains(pcqRecord1));
        assertTrue("The pcqRecord 2 is not found.", pcqIds.contains(pcqRecord2));
        assertFalse("The pcqRecord 3 is found.", pcqIds.contains(pcqRecord3));
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void assertPcqIdsProcessed(PcqAnswerResponse[] pcqAnswerRecords, String pcqRecord1, String pcqRecord2) {
        List<String> pcqIds = new ArrayList<>();
        for (PcqAnswerResponse answerResponse : pcqAnswerRecords) {
            pcqIds.add(answerResponse.getPcqId());
        }
        assertTrue("The pcqRecord 1 is not processed.", pcqIds.contains(pcqRecord1));
        assertTrue("The pcqRecord 2 is not processed.", pcqIds.contains(pcqRecord2));
    }

    protected PcqAnswerResponse getTestAnswerRecord(String pcqId, String apiUrl, String secretKey) throws IOException {
        return getResponseFromBackend(apiUrl, pcqId, secretKey);
    }
}
