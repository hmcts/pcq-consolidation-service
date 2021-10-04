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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.ConsolidationComponent;
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

    public static final String TEST_DIGITAL_CASE_TITLE = "Func-Test-Digital-Case-5";
    public static final String TEST_PAPER_CASE_TITLE = "Func-Test-Paper-Case-5";
    public static final String TEST_PCQ_ID_1 = "2ab8767c-f3e9-293b-afd4-128e1e57fc34";
    public static final String TEST_PCQ_ID_2 = "73c5a4de-932a-2093-851a-da3b99a70bba";
    public static final String TEST_PCQ_ID_3 = "b0ab25a9-a04d-2ba1-b9f5-3108b7b7884c";

    public static final String TEST_PAPER_CASE_DCN = "2020032244230034";

    @Value("${pcqBackendUrl}")
    private String pcqBackendUrl;

    @Value("${jwt_test_secret}")
    private String jwtSecretKey;

    @Autowired
    private ConsolidationComponent consolidationComponent;

    private CaseDetails pcqCase1;
    private CaseDetails pcqCase2;

    @Before
    public void createPcqData() throws IOException {

        // Create the PCQ answer records.
        createTestAnswerRecordWithoutCase(TEST_PCQ_ID_1);
        createTestAnswerRecordDcnWithoutCase(TEST_PCQ_ID_2);
        createTestAnswerRecordWithCase(TEST_PCQ_ID_3);

        // Create the Sample service core case data.
        pcqCase1 = createCcdPcqQuestionsDigitalCase(TEST_DIGITAL_CASE_TITLE, TEST_PCQ_ID_1);
        pcqCase2 = createCcdPcqQuestionsPaperCase(TEST_PAPER_CASE_TITLE, TEST_PAPER_CASE_DCN);
    }

    @After
    public void removePcqData() throws IOException {

        // Create the PCQ answer records.
        removeTestAnswerRecord(TEST_PCQ_ID_1);
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
        assertPcqIdsRetrieved(pcqAnswerRecords, TEST_PCQ_ID_1,TEST_PCQ_ID_2,TEST_PCQ_ID_3);

        //Check that the API - addCaseForPcq has been called and that the test records are updated.
        PcqAnswerResponse[] pcqRecordsProcessed = statusMap.get("PCQ_ID_PROCESSED");
        assertPcqIdsProcessed(pcqRecordsProcessed, TEST_PCQ_ID_1, TEST_PCQ_ID_2);

        //Make a call to the getAnswer from pcq backend to verify that case Id has been updated search by pcqId.
        PcqAnswerResponse answerResponse = getTestAnswerRecord(TEST_PCQ_ID_1, pcqBackendUrl, jwtSecretKey);
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

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void assertPcqIdsRetrieved(PcqAnswerResponse[] pcqAnswerRecords,
                                       String pcqRecord1, String pcqRecord2, String pcqRecord3) {
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
}
