package uk.gov.hmcts.reform.pcqconsolidationservice.ccd.util;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcqconsolidationservice.ccd.model.PcqQuestions;
import uk.gov.hmcts.reform.pcqconsolidationservice.ccd.model.ScannedDocument;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd.CcdAuthenticator;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd.CcdAuthenticatorFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;

@Slf4j
@Component
public class CaseCreator {

    private final CoreCaseDataApi feignCcdApi;
    private final CcdAuthenticatorFactory authenticatorFactory;
    private CcdAuthenticator authenticator;

    private static final long USER_TOKEN_REFRESH_IN_SECONDS = 300;

    private static final String TEST_CASE_JURISDICTION = "PCQTEST";
    private static final String TEST_CASE_TYPE_ID = "PCQQuestions";
    private static final String TEST_CREATE_CASE_EVENT_ID = "createCase";

    private static final String TEST_SAMPLE_DOC_FILENAME = "sample.pdf";
    private static final String TEST_SAMPLE_DOC_TYPE = "form";
    private static final String TEST_SAMPLE_DOC_SUBTYPE = "PA1A";
    private static final String TEST_SAMPLE_DOC_EXCEPTION_REFERENCE = "101010101";

    public static final String SEARCH_BY_CASE_NAME_QUERY_FORMAT =
            "{\"query\": { \"match_phrase\" : { \"data.text\" : \"%s\" }}}";

    public CaseCreator(
            CoreCaseDataApi feignCcdApi,
            CcdAuthenticatorFactory authenticatorFactory
    ) {
        this.feignCcdApi = feignCcdApi;
        this.authenticatorFactory = authenticatorFactory;
    }

    public CaseDetails createCase(PcqQuestions pcqQuestions) {
        CaseDetails pcqQuestionsCase = CaseDetails.builder().build();
        refreshExpiredIdamToken();

        try {
            StartEventResponse eventResponse = feignCcdApi.startForCaseworker(
                    authenticator.getUserToken(),
                    authenticator.getServiceToken(),
                    authenticator.getUserDetails().getId(),
                    TEST_CASE_JURISDICTION,
                    TEST_CASE_TYPE_ID,
                    TEST_CREATE_CASE_EVENT_ID
            );

            CaseDataContent caseDataContent = CaseDataContent.builder()
                    .data(pcqQuestions)
                    .event(Event
                            .builder()
                            .id(eventResponse.getEventId())
                            .summary("Case created")
                            .description("Functional Test Case PCQQuestions")
                            .build()
                    )
                    .eventToken(eventResponse.getToken())
                    .build();

            pcqQuestionsCase = feignCcdApi.submitForCaseworker(
                    authenticator.getUserToken(),
                    authenticator.getServiceToken(),
                    authenticator.getUserDetails().getId(),
                    TEST_CASE_JURISDICTION,
                    TEST_CASE_TYPE_ID,
                    true,
                    caseDataContent
            );

        } catch (FeignException exception) {
            log.error("Unable to create test case", exception);
        }

        return pcqQuestionsCase;
    }

    public Optional<CaseDetails> findCase(String caseName) {
        refreshExpiredIdamToken();

        try {
            SearchResult searchResult = feignCcdApi.searchCases(
                    authenticator.getUserToken(),
                    authenticator.getServiceToken(),
                    TEST_CASE_TYPE_ID,
                    format(SEARCH_BY_CASE_NAME_QUERY_FORMAT, caseName)
            );

            return searchResult.getCases().stream().findFirst();

        } catch (FeignException exception) {
            log.error("Unable to find test case", exception);
        }

        return Optional.empty();
    }

    public ScannedDocument createScannedDocument(String dcn) {
        return ScannedDocument.builder()
                .controlNumber(dcn)
                .type(TEST_SAMPLE_DOC_TYPE)
                .fileName(TEST_SAMPLE_DOC_FILENAME)
                .deliveryDate(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDateTime())
                .scannedDate(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDateTime())
                .subtype(TEST_SAMPLE_DOC_SUBTYPE)
                .exceptionReference(TEST_SAMPLE_DOC_EXCEPTION_REFERENCE)
                .build();
    }

    private void refreshExpiredIdamToken() {
        if (this.authenticator == null
                || this.authenticator.userTokenAgeInSeconds() > USER_TOKEN_REFRESH_IN_SECONDS) {
            this.authenticator = authenticatorFactory.createCcdAuthenticator();
        }
    }
}
