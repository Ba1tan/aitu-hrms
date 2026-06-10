package kz.aitu.hrms.integration.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeIinDto {
    private UUID employeeId;
    private String iin;
    private String fullName;
    /** KZ IBAN — populated from the employee-service detail response. */
    private String bankAccount;
    private String bankName;
}
