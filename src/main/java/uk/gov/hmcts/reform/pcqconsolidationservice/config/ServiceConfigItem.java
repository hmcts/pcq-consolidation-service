package uk.gov.hmcts.reform.pcqconsolidationservice.config;

import java.util.List;
import java.util.Locale;
import javax.validation.constraints.NotNull;

public class ServiceConfigItem {

    @NotNull
    private String service;

    @NotNull
    private List<String> caseTypeIds;

    private List<CaseFieldMapping> caseFieldMappings;

    private String caseDcnDocumentMapping;

    private String caseDcnDocumentSuffix;

    public String getService() {
        return service.toUpperCase(Locale.ENGLISH);
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<String> getCaseTypeIds() {
        return caseTypeIds;
    }

    public List<CaseFieldMapping> getCaseFieldMappings() {
        return caseFieldMappings;
    }

    public String getCaseField(String actor) {
        String caseField;
        CaseFieldMapping caseFieldMapping = caseFieldMappings == null ? null :
                this.caseFieldMappings.stream()
                    .filter(a -> actor.equalsIgnoreCase(a.getActor()))
                    .findAny()
                    .orElse(null);
        caseField = caseFieldMapping == null ? null : caseFieldMapping.getName();
        return caseField;
    }

    public void setCaseTypeIds(List<String> caseTypeIds) {
        this.caseTypeIds = caseTypeIds;
    }

    public void setCaseFieldMappings(List<CaseFieldMapping> caseFieldMappings) {
        this.caseFieldMappings = caseFieldMappings;
    }

    public void setCaseDcnDocumentMapping(String caseDcnDocumentMapping) {
        this.caseDcnDocumentMapping = caseDcnDocumentMapping;
    }

    public String getCaseDcnDocumentMapping() {
        return caseDcnDocumentMapping;
    }

    public void setCaseDcnDocumentSuffix(String caseDcnDocumentSuffix) {
        this.caseDcnDocumentSuffix = caseDcnDocumentSuffix;
    }

    public String getCaseDcnDocumentSuffix() {
        return caseDcnDocumentSuffix;
    }
}
