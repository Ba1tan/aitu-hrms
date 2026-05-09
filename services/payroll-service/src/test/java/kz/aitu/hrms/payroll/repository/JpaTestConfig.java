package kz.aitu.hrms.payroll.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

/**
 * Wires JPA auditing and limits the scan to the payroll packages so
 * {@code @DataJpaTest} doesn't try to instantiate the full application.
 */
@TestConfiguration
@EntityScan(basePackages = "kz.aitu.hrms.payroll.entity")
@EnableJpaRepositories(basePackages = "kz.aitu.hrms.payroll.repository")
@EnableJpaAuditing
public class JpaTestConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test");
    }
}