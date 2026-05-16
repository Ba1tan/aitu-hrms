package kz.aitu.hrms.integration.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class SetupStatusDto {
    private boolean configured;
    private int totalRequired;
    private List<String> missingRequired;
    private boolean explicitlyCompleted;
}
