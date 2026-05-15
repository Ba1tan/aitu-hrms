package kz.aitu.hrms.reporting.config;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Configuration
public class ReportingConfig {

    @Value("${reporting.tmp-dir:/tmp/hrms-reports}")
    private String tmpDir;

    @Bean
    public BaseFont cyrillicFont() throws IOException, DocumentException {
        File dir = new File(tmpDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        InputStream fontStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream();
        File tempFont = new File(dir, "DejaVuSans.ttf");
        try (InputStream in = fontStream) {
            Files.copy(in, tempFont.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return BaseFont.createFont(tempFont.getAbsolutePath(),
                BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    }
}
