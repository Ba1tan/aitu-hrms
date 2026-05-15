package kz.aitu.hrms.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayslipDto {
    private UUID id;
    private UUID employeeId;
    private String employeeFirstName;
    private String employeeLastName;
    private String periodName;
    private BigDecimal grossSalary;
    private BigDecimal opv;
    private BigDecimal vosms;
    private BigDecimal ipn;
    private BigDecimal netSalary;
}
