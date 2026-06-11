package kz.aitu.hrms.leave.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Read-side calls to integration-hub for non-sensitive company settings.
 * Used to resolve the carryover cap (`leave.annual_carryover_max_pct`) at
 * runtime instead of baking it into this service's config.
 *
 * Hits the unauthenticated-readable {@code /settings/public} endpoint; the
 * caller's JWT is forwarded by {@link kz.aitu.hrms.leave.config.FeignConfig}.
 * Only the fields the leave flow needs are decoded — unknown JSON properties
 * are ignored by the default decoder.
 */
@FeignClient(
        name = "integration-hub",
        url = "${app.services.integration-hub-uri}",
        path = "/api/v1"
)
public interface SettingsClient {

    @GetMapping("/settings/public")
    Envelope<List<SettingDto>> getPublic();

    record SettingDto(String key, String value) {}

    record Envelope<T>(boolean success, String message, T data) {}
}
