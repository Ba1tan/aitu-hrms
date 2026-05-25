package kz.aitu.hrms.reporting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Request;
import feign.Response;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
import kz.aitu.hrms.reporting.client.dto.LeaveBalanceDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.client.dto.PayrollTotalsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Feign decoder unwraps the shared {@code ApiResponse<T>} envelope.
 * This is the bug that emptied every report: the inner {@code data} payload must
 * be bound to the client return type, not the wrapper.
 */
class EnvelopeDecoderTest {

    private EnvelopeDecoder decoder;

    @BeforeEach
    void setUp() {
        // Mirrors the Spring Boot-configured mapper: java.time support on.
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        decoder = new EnvelopeDecoder(mapper);
    }

    @Test
    void unwrapsEnvelopeIntoPageResponse() throws Exception {
        String body = """
            {"success":true,"message":"OK","data":{
               "content":[{"fullName":"Нурлан Сейткали",
                           "status":"ACTIVE","hireDate":"2023-03-01"}],
               "last":true,"number":0},
             "timestamp":"2026-05-25T10:00:00"}
            """;

        @SuppressWarnings("unchecked")
        PageResponse<EmployeeSummaryDto> page =
                (PageResponse<EmployeeSummaryDto>) decoder.decode(
                        response(body), pageOf(EmployeeSummaryDto.class));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getFullName()).isEqualTo("Нурлан Сейткали");
        assertThat(page.getContent().get(0).getHireDate().toString()).isEqualTo("2023-03-01");
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void unwrapsEnvelopeIntoList() throws Exception {
        String body = """
            {"success":true,"data":[{"leaveType":{"id":"%s","name":"Annual"},"remainingDays":12}]}
            """.formatted(UUID.randomUUID());

        @SuppressWarnings("unchecked")
        List<LeaveBalanceDto> balances =
                (List<LeaveBalanceDto>) decoder.decode(response(body), listOf(LeaveBalanceDto.class));

        assertThat(balances).hasSize(1);
        assertThat(balances.get(0).getLeaveTypeName()).isEqualTo("Annual");
    }

    @Test
    void unwrapsEnvelopeIntoSingleDto() throws Exception {
        String body = """
            {"success":true,"data":{"totalGross":100.50,"totalNet":80.00,"employeeCount":3}}
            """;

        PayrollTotalsDto totals =
                (PayrollTotalsDto) decoder.decode(response(body), PayrollTotalsDto.class);

        assertThat(totals.getEmployeeCount()).isEqualTo(3);
        assertThat(totals.getTotalGross()).isEqualByComparingTo("100.50");
    }

    @Test
    void passesThroughNonEnvelopedBody() throws Exception {
        String body = """
            {"totalGross":5,"totalNet":4,"employeeCount":1}
            """;

        PayrollTotalsDto totals =
                (PayrollTotalsDto) decoder.decode(response(body), PayrollTotalsDto.class);

        assertThat(totals.getEmployeeCount()).isEqualTo(1);
    }

    @Test
    void nullDataDecodesToNull() throws Exception {
        String body = """
            {"success":true,"message":"deleted","data":null}
            """;

        Object result = decoder.decode(response(body), PayrollTotalsDto.class);

        assertThat(result).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Response response(String body) {
        Request request = Request.create(
                Request.HttpMethod.GET, "/", Collections.emptyMap(),
                Request.Body.empty(), null);
        return Response.builder()
                .status(200)
                .reason("OK")
                .request(request)
                .headers(Collections.emptyMap())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    private Type pageOf(Class<?> element) {
        return new ParameterizedType() {
            public Type[] getActualTypeArguments() { return new Type[]{element}; }
            public Type getRawType() { return PageResponse.class; }
            public Type getOwnerType() { return null; }
        };
    }

    private Type listOf(Class<?> element) {
        return new ParameterizedType() {
            public Type[] getActualTypeArguments() { return new Type[]{element}; }
            public Type getRawType() { return List.class; }
            public Type getOwnerType() { return null; }
        };
    }
}
