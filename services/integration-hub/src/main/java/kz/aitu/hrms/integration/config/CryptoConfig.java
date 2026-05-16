package kz.aitu.hrms.integration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

@Configuration
public class CryptoConfig {

    @Bean
    @Primary
    public TextEncryptor settingsEncryptor(@Value("${app.jwt.secret}") String jwtSecret) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        String salt = HexFormat.of().formatHex(Arrays.copyOf(keyBytes, 16));
        return Encryptors.delux(jwtSecret, salt);
    }
}
