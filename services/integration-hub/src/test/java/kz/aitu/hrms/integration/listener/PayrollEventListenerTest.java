package kz.aitu.hrms.integration.listener;

import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import kz.aitu.hrms.common.event.PayrollPeriodApprovedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.service.SettingsService;
import kz.aitu.hrms.integration.service.SyncOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollEventListenerTest {

    @Mock private SettingsService settings;
    @Mock private SyncOrchestrator orchestrator;
    @InjectMocks private PayrollEventListener listener;

    @Test
    void onPayrollPeriodApproved_blankBaseUrl_skips() {
        when(settings.getOrDefault("integration.1c_base_url", "")).thenReturn("");
        PayrollPeriodApprovedEvent event = PayrollPeriodApprovedEvent.builder()
                .periodId(UUID.randomUUID()).approvedBy(UUID.randomUUID()).build();

        listener.onPayrollPeriodApproved(event);

        verify(orchestrator, never()).trigger(any(), any());
    }

    @Test
    void onPayrollPeriodApproved_alreadyRunning_swallowsBusinessException() {
        when(settings.getOrDefault("integration.1c_base_url", "")).thenReturn("http://1c.example.com");
        PayrollPeriodApprovedEvent event = PayrollPeriodApprovedEvent.builder()
                .periodId(UUID.randomUUID()).approvedBy(UUID.randomUUID()).build();
        doThrow(new BusinessException("already running")).when(orchestrator).trigger(any(), any());

        listener.onPayrollPeriodApproved(event);

        verify(orchestrator).trigger(any(), any());
    }

    @Test
    void onPayrollPeriodApproved_unexpectedException_swallowed() {
        when(settings.getOrDefault("integration.1c_base_url", "")).thenReturn("http://1c.example.com");
        PayrollPeriodApprovedEvent event = PayrollPeriodApprovedEvent.builder()
                .periodId(UUID.randomUUID()).approvedBy(UUID.randomUUID()).build();
        doThrow(new RuntimeException("DB down")).when(orchestrator).trigger(any(), any());

        listener.onPayrollPeriodApproved(event);

        verify(orchestrator).trigger(any(), any());
    }

    @Test
    void onPayrollJobCompleted_exceptionSwallowed() {
        when(settings.getOrDefault("integration.auto_sync_on_complete", "false"))
                .thenThrow(new RuntimeException("settings unavailable"));
        PayrollJobCompletedEvent event = PayrollJobCompletedEvent.builder()
                .periodId(UUID.randomUUID()).build();

        listener.onPayrollJobCompleted(event);

        // exception was swallowed, no further calls expected
    }
}
