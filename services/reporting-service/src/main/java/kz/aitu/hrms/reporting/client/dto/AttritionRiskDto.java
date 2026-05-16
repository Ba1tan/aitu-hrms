package kz.aitu.hrms.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttritionRiskDto {
    private UUID employeeId;
    private String employeeName;
    private String department;
    private double riskScore;
    private String riskLevel;
    private String reason;
}
