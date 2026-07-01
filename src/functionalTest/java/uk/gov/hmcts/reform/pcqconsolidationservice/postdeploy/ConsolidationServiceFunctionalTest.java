package uk.gov.hmcts.reform.pcqconsolidationservice.postdeploy;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.ConsolidationComponent;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.TestApplicationConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplicationConfiguration.class)
@ActiveProfiles("functional")
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
class ConsolidationServiceFunctionalTest extends ConsolidationServiceTestBase {

    private static final String TEST_DIGITAL_CASE_TITLE = "Digital-Case-Functional-Tests-1";
    private static final String TEST_PAPER_CASE_TITLE = "Paper-Case-Functional-Tests-1";

    protected String testPcqId1 = UUID.randomUUID().toString();
    protected String testPcqId2 = UUID.randomUUID().toString();
    protected String testPcqId3 = UUID.randomUUID().toString();
    protected String testDcn = generateDcn();

    @Value("${pcqBackendUrl}")
    private String pcqBackendUrl;

    @Value("${jwt_test_secret}")
    private String jwtSecretKey;

    @Autowired
    private ConsolidationComponent consolidationComponent;

    private CaseDetails pcqCase1;
    private CaseDetails pcqCase2;

    @BeforeEach
    void createPcqData() throws IOException {
        // Create the Sample service core case data.

        pcqCase1 = createCcdPcqQuestionsDigitalCase(TEST_DIGITAL_CASE_TITLE, testPcqId1);
        pcqCase2 = createCcdPcqQuestionsPaperCase(TEST_PAPER_CASE_TITLE, testDcn);

        // Collect PCQ from first CCD case.
        testPcqId1 = (String)pcqCase1.getData().get("pcqId");
        createTestAnswerRecordWithoutCase(testPcqId1);

        // Collect DCN from second CCD case.
        Map doc = (LinkedHashMap)((ArrayList)pcqCase2.getData().get("scannedDocuments")).get(0);
        testDcn = ((LinkedHashMap)doc.get("value")).get("controlNumber").toString();

        // Create PCQ records in database.
        createTestAnswerRecordDcnWithoutCase(testPcqId2, testDcn);
        createTestAnswerRecordWithCase(testPcqId3);
    }

    @AfterEach
    void removePcqData() {

        // Remove the PCQ answer records.
        removeTestAnswerRecord(testPcqId1);
        removeTestAnswerRecord(testPcqId2);
        removeTestAnswerRecord(testPcqId3);
    }


    @SuppressWarnings("unchecked")
    @Test
    void testExecuteMethod() {

        //Invoke the executor
        consolidationComponent.execute();


        //Get the status map accessible from the ConsolidationComponent.
        Map<String, PcqAnswerResponse[]> statusMap = consolidationComponent.getPcqIdsMap();
        //Check that the API - pcqWithoutCase has been called and that the test records are found
        Assertions.assertNotNull(statusMap, "Status Map is null");
        PcqAnswerResponse[] pcqAnswerRecords = statusMap.get("PCQ_ID_FOUND");
        assertPcqIdsRetrieved(pcqAnswerRecords, testPcqId1, testPcqId2, testPcqId3);

        //Check that the API - addCaseForPcq has been called and that the test records are updated.
        PcqAnswerResponse[] pcqRecordsProcessed = statusMap.get("PCQ_ID_PROCESSED");
        assertPcqIdsProcessed(pcqRecordsProcessed, testPcqId1, testPcqId2);

        //Make a call to the getAnswer from pcq backend to verify that case Id has been updated search by pcqId.
        PcqAnswerResponse answerResponse = getTestAnswerRecord(testPcqId1, pcqBackendUrl, jwtSecretKey);
        Assertions.assertNotNull(answerResponse, "The get response is not null");
        Assertions.assertEquals(
                pcqCase1.getId().toString(), answerResponse.getCaseId(), "The get response matches ccd case id");

        //Make a call to the getAnswer from pcq backend to verify that case Id has been updated search by dcn.
        answerResponse = getTestAnswerRecord(testPcqId2, pcqBackendUrl, jwtSecretKey);
        Assertions.assertNotNull(answerResponse, "The get response is not null");
        Assertions.assertEquals(
                pcqCase2.getId().toString(), answerResponse.getCaseId(), "The get response matches ccd case id");
    }

    private void createTestAnswerRecordWithoutCase(String pcqId) throws IOException {
        createTestAnswerRecord("JsonTestFiles/FirstSubmitAnswer.json", pcqBackendUrl, pcqId,
                jwtSecretKey,null);
    }

    private void createTestAnswerRecordDcnWithoutCase(String pcqId,String dcnNumber) throws IOException {
        createTestAnswerRecord("JsonTestFiles/SecondSubmitAnswerWithDcn.json", pcqBackendUrl, pcqId,
                jwtSecretKey,dcnNumber);
    }

    private void createTestAnswerRecordWithCase(String pcqId) throws IOException {
        createTestAnswerRecord("JsonTestFiles/FirstSubmitAnswerWithCase.json", pcqBackendUrl, pcqId,
                jwtSecretKey,null);
    }

    private void removeTestAnswerRecord(String pcqId) {
        removeTestAnswerRecord(pcqBackendUrl, pcqId, jwtSecretKey);
    }

    protected void removeTestAnswerRecord(String apiUrl, String pcqId, String jwtSecretKey) {
        deleteTestRecordFromBackend(apiUrl, pcqId, jwtSecretKey);
    }

    private void assertPcqIdsRetrieved(PcqAnswerResponse[] pcqAnswerRecords,
                                       String pcqRecord1,String pcqRecord2,
                                       String pcqRecord3) {

        Set<String> pcqIds = Arrays.stream(pcqAnswerRecords)
                .map(PcqAnswerResponse::getPcqId)
                .collect(Collectors.toSet());

        Assertions.assertTrue(pcqIds.contains(pcqRecord1), "The pcqRecord 1 is not found.");
        Assertions.assertTrue(pcqIds.contains(pcqRecord2), "The pcqRecord 2 is not found.");
        Assertions.assertFalse(pcqIds.contains(pcqRecord3), "The pcqRecord 3 is found.");
    }

    private void assertPcqIdsProcessed(PcqAnswerResponse[] pcqAnswerRecords, String pcqRecord1, String pcqRecord2) {
        List<String> pcqIds = new ArrayList<>();
        for (PcqAnswerResponse answerResponse : pcqAnswerRecords) {
            pcqIds.add(answerResponse.getPcqId());
        }
        Assertions.assertTrue(pcqIds.contains(pcqRecord1), "The pcqRecord 1 is not processed.");
        Assertions.assertTrue(pcqIds.contains(pcqRecord2), "The pcqRecord 2 is not processed.");
    }

    protected PcqAnswerResponse getTestAnswerRecord(String pcqId, String apiUrl, String secretKey) {
        return getResponseFromBackend(apiUrl, pcqId, secretKey);
    }

    protected String generateDcn() {
        // numberOfDigits must be < 10
        int numberOfDigits = 8;
        int member = (int) Math.pow(10, numberOfDigits - 1);
        int dcn = member + new Random().nextInt(9 * member);
        return Integer.toString(dcn);
    }
}
