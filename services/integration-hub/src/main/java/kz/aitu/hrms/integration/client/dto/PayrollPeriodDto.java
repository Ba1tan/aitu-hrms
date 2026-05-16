package kz.aitu.hrms.integration.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollPeriodDto {
    private UUID id;
    private Integer year;
    private Integer month;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer workingDays;
}
