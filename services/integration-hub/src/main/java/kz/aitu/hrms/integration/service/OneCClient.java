package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class OneCClient {

    private final RestTemplate oneCRestTemplate;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final SettingsService settingsService;

    public OneCClient(@Qualifier("oneCRestTemplate") RestTemplate oneCRestTemplate,
                      CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                      SettingsService settingsService) {
        this.oneCRestTemplate = oneCRestTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.settingsService = settingsService;
    }

    public OneCResponse sendPayroll(String jsonPayload) {
        String baseUrl = settingsService.getOrDefault("integration.1c_base_url", "");
        if (baseUrl.isBlank()) {
            log.info("1C sync skipped — integration.1c_base_url not configured");
            OneCResponse skipped = new OneCResponse();
            skipped.setStatus("SKIPPED");
            skipped.setMessage("1C base URL not configured");
            return skipped;
        }

        String url = baseUrl + "/hs/hrms/payroll/sync";
        String username = settingsService.getOrDefault("integration.1c_username", "");
        String password = settingsService.getOrDefault("integration.1c_password", "");

        CircuitBreaker cb = circuitBreakerFactory.create("onec");

        return cb.run(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (!username.isBlank()) {
                headers.setBasicAuth(username, password);
            }

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<OneCResponse> response = oneCRestTemplate.exchange(
                    url, HttpMethod.POST, request, OneCResponse.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException("1C returned " + response.getStatusCode());
            }
            return response.getBody();
        }, throwable -> {
            throw new BusinessException("1C circuit breaker open or call failed: " + throwable.getMessage());
        });
    }
}
