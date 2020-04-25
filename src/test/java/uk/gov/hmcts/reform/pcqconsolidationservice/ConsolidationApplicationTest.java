package uk.gov.hmcts.reform.pcqconsolidationservice;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pcqconsolidationservice.exception.ExternalApiException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ConsolidationApplicationTest {

    @InjectMocks
    private ConsolidationApplication testConsolidationApplication;

    @Mock
    private ConsolidationComponent testConsolidationComponent;

    @Mock
    private TelemetryClient client;

    @Test
    public void testApplicationExecuted() throws Exception {
        testConsolidationApplication.run(null);
        verify(testConsolidationComponent, times(1)).execute();
        verify(client, times(1)).flush();
    }

    @Test
    public void testExceptionPropagated() throws Exception {
        doThrow(new ExternalApiException(HttpStatus.BAD_REQUEST, "Not available")).when(testConsolidationComponent)
                .execute();
        assertThrows(ExternalApiException.class, () -> testConsolidationApplication.run(null));
        verify(testConsolidationComponent, times(1)).execute();
    }
}
