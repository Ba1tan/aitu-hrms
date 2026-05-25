package kz.aitu.hrms.reporting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "kz.aitu.hrms.reporting.client")
public class FeignConfig {

    /**
     * Replaces the default Feign decoder so every client unwraps the shared
     * {@code ApiResponse<T>} envelope returned by the other services. Reuses the
     * Spring-managed {@link ObjectMapper} (with {@code JavaTimeModule}) so
     * {@code LocalDate}/{@code BigDecimal} fields deserialize correctly.
     */
    @Bean
    public Decoder feignDecoder(ObjectMapper mapper) {
        return new EnvelopeDecoder(mapper);
    }
}
