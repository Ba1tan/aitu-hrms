package kz.aitu.hrms.reporting.client;

import kz.aitu.hrms.reporting.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "leave-client", url = "${clients.leave-service}")
public interface LeaveClient {

    @GetMapping("/api/v1/leave/requests/pending/count")
    LeavePendingCountDto pendingCount();

    @GetMapping("/api/v1/leave/requests/pending")
    PageResponse<Object> pendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1") int size);

    @GetMapping("/api/v1/leave/balances")
    List<LeaveBalanceDto> myBalances();

    @GetMapping("/api/v1/leave/balances/employee/{id}")
    List<LeaveBalanceDto> balancesFor(@PathVariable UUID id, @RequestParam int year);
}
