package uk.gov.hmcts.reform.pcqconsolidationservice.exception;

import feign.FeignException;

public class ServiceFeignException extends FeignException {

    private static final long serialVersionUID = -422128114862699006L;

    public ServiceFeignException(int status, String message) {
        super(status, message);
    }
}
