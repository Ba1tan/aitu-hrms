package kz.aitu.hrms.reporting.controller.advice;

import feign.FeignException;
import feign.Request;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import kz.aitu.hrms.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReportingExceptionHandlerTest {

    private final ReportingExceptionHandler handler = new ReportingExceptionHandler();

    @Test
    void feignException_returns502() {
        Request req = Request.create(Request.HttpMethod.GET, "http://test", Map.of(), null, StandardCharsets.UTF_8, null);
        FeignException ex = FeignException.errorStatus("test", feign.Response.builder()
                .status(503).reason("down").request(req).headers(Map.of()).build());

        ResponseEntity<ApiResponse<Void>> resp = handler.upstream(ex);
        assertThat(resp.getStatusCode().value()).isEqualTo(502);
    }

    @Test
    void constraintViolation_returns400() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());
        ResponseEntity<ApiResponse<Void>> resp = handler.validation(ex);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void illegalArgument_returns400() {
        ResponseEntity<ApiResponse<Void>> resp = handler.badRequest(new IllegalArgumentException("bad"));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void accessDenied_returns403() {
        ResponseEntity<ApiResponse<Void>> resp = handler.forbidden(new AccessDeniedException("no"));
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void genericException_returns500() {
        ResponseEntity<ApiResponse<Void>> resp = handler.other(new RuntimeException("boom"));
        assertThat(resp.getStatusCode().value()).isEqualTo(500);
    }
}
