package uk.gov.hmcts.reform.pcqconsolidationservice.config;

import jakarta.validation.constraints.NotNull;

public class CaseFieldMapping {

    @NotNull
    private String actor;

    @NotNull
    private String name;

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}