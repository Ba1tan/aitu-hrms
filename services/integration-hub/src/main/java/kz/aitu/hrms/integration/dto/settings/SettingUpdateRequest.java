package kz.aitu.hrms.integration.dto.settings;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SettingUpdateRequest {
    @NotBlank
    private String value;
}
