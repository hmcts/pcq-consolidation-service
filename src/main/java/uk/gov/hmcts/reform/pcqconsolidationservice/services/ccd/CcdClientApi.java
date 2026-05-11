package uk.gov.hmcts.reform.pcqconsolidationservice.services.ccd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigItem;
import uk.gov.hmcts.reform.pcqconsolidationservice.config.ServiceConfigProvider;

import java.util.List;

import static java.util.Collections.emptyList;

@Slf4j
@Component
@SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
public class CcdClientApi {

    private final CoreCaseDataApi feignCcdApi;
    private final ServiceConfigProvider serviceConfigProvider;
    private final CcdAuthenticatorFactory authenticatorFactory;
    private CcdAuthenticator authenticator;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final long USER_TOKEN_REFRESH_IN_SECONDS = 300;

    public static final String SEARCH_BY_PCQ_ID_DEFAULT_FIELD_NAME = "pcqId";

    public static final String SEARCH_BY_DCN_DEFAULT_FIELD_NAME = "scannedDocuments.value.controlNumber";

    public CcdClientApi(
            CoreCaseDataApi feignCcdApi,
            CcdAuthenticatorFactory authenticatorFactory,
            ServiceConfigProvider serviceConfigProvider
    ) {
        this.feignCcdApi = feignCcdApi;
        this.authenticatorFactory = authenticatorFactory;
        this.serviceConfigProvider = serviceConfigProvider;
    }

    public List<Long> getCaseRefsByPcqId(String pcqId, String service, String actor) throws Exception {

        refreshExpiredIdamToken();
        ServiceConfigItem serviceConfig = serviceConfigProvider.getConfig(service);

        if (serviceConfig.getCaseTypeIds().isEmpty()) {
            log.info(
                    "Skipping case search by pcq ID ({}) for service {} because it has no case type ID configured",
                    pcqId,
                    service
            );
            return emptyList();

        } else {
            String caseTypeIdsStr = String.join(",", serviceConfig.getCaseTypeIds());
            String caseFieldNamePcqId = serviceConfig.getCaseField(actor) == null
                    ? SEARCH_BY_PCQ_ID_DEFAULT_FIELD_NAME : serviceConfig.getCaseField(actor);
            String searchQuery = buildEsMatchPhraseQuery(caseFieldNamePcqId, pcqId);
            log.debug(
                    "Searching for pcqId {} within the service {} using ES query {}",
                    pcqId,
                    service,
                    searchQuery
            );
            SearchResult searchResult = feignCcdApi.searchCases(
                    authenticator.getUserToken(),
                    authenticator.getServiceToken(),
                    caseTypeIdsStr,
                    searchQuery
            );
            return searchResult.getCases().stream().map(CaseDetails::getId).toList();
        }
    }

    public List<Long> getCaseRefsByOriginatingFormDcn(String dcn, String service) throws Exception {

        refreshExpiredIdamToken();
        ServiceConfigItem serviceConfig = serviceConfigProvider.getConfig(service);

        if (serviceConfig.getCaseTypeIds().isEmpty()) {
            log.debug(
                    "Skipping case search by DCN ({}) for service {} because it has no case type ID configured",
                    dcn,
                    service
            );
            return emptyList();

        } else {
            String caseTypeIdsStr = String.join(",", serviceConfig.getCaseTypeIds());
            String dcnSearch = serviceConfig.getCaseDcnDocumentSuffix() == null
                    ? dcn : dcn + serviceConfig.getCaseDcnDocumentSuffix();
            String caseDocumentDcn = serviceConfig.getCaseDcnDocumentMapping() == null
                    ? SEARCH_BY_DCN_DEFAULT_FIELD_NAME : serviceConfig.getCaseDcnDocumentMapping();
            String searchQuery = buildEsMatchPhraseQuery(caseDocumentDcn, dcnSearch);
            log.debug(
                    "Searching for DCN {} within the service {} using ES query {}",
                    dcn,
                    service,
                     searchQuery
            );
            SearchResult searchResult = feignCcdApi.searchCases(
                    authenticator.getUserToken(),
                    authenticator.getServiceToken(),
                    caseTypeIdsStr,
                    searchQuery
            );
            return searchResult.getCases().stream().map(CaseDetails::getId).toList();
        }
    }

    static String buildEsMatchPhraseQuery(String fieldName, String value) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ObjectNode matchPhrase = root.putObject("query").putObject("match_phrase");
        matchPhrase.put("data." + fieldName, value);
        return root.toString();
    }

    private void refreshExpiredIdamToken() {
        if (this.authenticator == null
                || this.authenticator.userTokenAgeInSeconds() > USER_TOKEN_REFRESH_IN_SECONDS) {
            this.authenticator = authenticatorFactory.createCcdAuthenticator();
        }
    }

    public void refreshToken() {
        this.authenticator = authenticatorFactory.createCcdAuthenticator();
    }
}

