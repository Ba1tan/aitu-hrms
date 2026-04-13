package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAttemptDetectedEvent {
    private UUID employeeId;
    private double fraudScore;
    private String flags;
    private String deviceId;
}
