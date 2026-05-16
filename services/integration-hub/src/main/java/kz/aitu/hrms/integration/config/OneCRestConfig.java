package kz.aitu.hrms.integration.config;

import kz.aitu.hrms.integration.service.SettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OneCRestConfig {

    @Value("${integration.onec.connect-timeout-seconds:10}")
    private int connectTimeout;

    @Value("${integration.onec.read-timeout-seconds:60}")
    private int readTimeout;

    @Bean(name = "oneCRestTemplate")
    public RestTemplate oneCRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(connectTimeout))
                .setReadTimeout(Duration.ofSeconds(readTimeout))
                .build();
    }
}
