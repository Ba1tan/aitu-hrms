package kz.aitu.hrms.employee.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.openfeign.support.SpringEncoder;

/**
 * Enables multipart/form-data support for Feign clients so
 * {@link kz.aitu.hrms.employee.client.AiMlClient#enrollFace} can stream
 * {@link org.springframework.web.multipart.MultipartFile}s to ai-ml-service.
 */
@Configuration
public class FeignConfig {

    @Bean
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }
}