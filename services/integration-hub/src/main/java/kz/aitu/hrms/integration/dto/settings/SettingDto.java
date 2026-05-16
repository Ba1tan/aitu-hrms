package kz.aitu.hrms.integration.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @AllArgsConstructor @NoArgsConstructor
public class SettingDto {
    private UUID id;
    private String key;
    private String value;
    private String description;
    private String category;
    private LocalDateTime updatedAt;
}
