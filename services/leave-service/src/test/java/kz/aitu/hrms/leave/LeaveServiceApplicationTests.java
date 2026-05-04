package kz.aitu.hrms.leave;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.openfeign.client.config.default.connectTimeout=100",
        "spring.cloud.openfeign.client.config.default.readTimeout=100"
})
class LeaveServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}