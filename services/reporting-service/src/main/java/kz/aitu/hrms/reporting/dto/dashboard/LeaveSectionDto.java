package kz.aitu.hrms.reporting.dto.dashboard;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveSectionDto implements Serializable {
    private long pendingLeaveRequests;
}
