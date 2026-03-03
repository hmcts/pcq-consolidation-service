package uk.gov.hmcts.reform.pcqconsolidationservice.services.pcqbackend.impl;

import feign.FeignException;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcq.commons.controller.feign.PcqBackendFeignClient;
import uk.gov.hmcts.reform.pcq.commons.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcq.commons.model.PcqRecordWithoutCaseResponse;
import uk.gov.hmcts.reform.pcq.commons.model.SubmitResponse;
import uk.gov.hmcts.reform.pcq.commons.utils.JsonFeignResponseUtil;
import uk.gov.hmcts.reform.pcqconsolidationservice.services.pcqbackend.PcqBackendService;

import java.io.IOException;

@Slf4j
@Service
public class PcqBackendServiceImpl implements PcqBackendService {

    private final PcqBackendFeignClient pcqBackendFeignClient;
    private final AuthTokenGenerator authTokenGenerator;

    @Value("${coRelationId:Test}")
    private String coRelationHeader;

    @Autowired
    public PcqBackendServiceImpl(PcqBackendFeignClient pcqBackendFeignClient,
                                 AuthTokenGenerator authTokenGenerator) {
        this.pcqBackendFeignClient = pcqBackendFeignClient;
        this.authTokenGenerator = authTokenGenerator;
    }

    @Override
    @SuppressWarnings({"PMD.PreserveStackTrace", "PMD.DataflowAnomalyAnalysis", "unchecked"})
    public ResponseEntity<PcqRecordWithoutCaseResponse> getPcqWithoutCase() {
        ResponseEntity<PcqRecordWithoutCaseResponse> responseEntity;

        String serviceAuthorization = authTokenGenerator.generate();
        try (Response response = pcqBackendFeignClient.getPcqWithoutCase(coRelationHeader, serviceAuthorization)) {
            if (response.headers() != null && response.headers().size() > 0) {
                java.util.Collection<String> contentTypes = response.headers().get("Content-Type");
                for (String contentType : contentTypes) {
                    log.info("Response contentType: " + contentType);
                }
            }
            responseEntity = JsonFeignResponseUtil.toResponseEntity(response, PcqRecordWithoutCaseResponse.class);
        } catch (FeignException ex) {
            throw new ExternalApiException(HttpStatus.valueOf(ex.status()), ex.getMessage());
        } catch (IOException ioe) {
            throw new ExternalApiException(HttpStatus.SERVICE_UNAVAILABLE, ioe.getMessage());
        }

        return responseEntity;

    }

    @Override
    @SuppressWarnings({"PMD.PreserveStackTrace", "PMD.DataflowAnomalyAnalysis", "unchecked"})
    public ResponseEntity<SubmitResponse> addCaseForPcq(String pcqId, String caseId) {
        ResponseEntity<SubmitResponse> responseEntity;

        String serviceAuthorization = authTokenGenerator.generate();
        try (Response response = pcqBackendFeignClient.addCaseForPcq(
                coRelationHeader, serviceAuthorization, pcqId, caseId)) {
            responseEntity = JsonFeignResponseUtil.toResponseEntity(response, SubmitResponse.class);
        } catch (FeignException ex) {
            throw new ExternalApiException(HttpStatus.valueOf(ex.status()), ex.getMessage());
        } catch (IOException ioe) {
            throw new ExternalApiException(HttpStatus.SERVICE_UNAVAILABLE, ioe.getMessage());
        }

        return responseEntity;
    }


}
