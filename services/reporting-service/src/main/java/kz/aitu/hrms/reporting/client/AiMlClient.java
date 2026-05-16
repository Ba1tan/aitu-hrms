package kz.aitu.hrms.reporting.client;

import kz.aitu.hrms.reporting.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "ai-ml-client", url = "${clients.ai-ml-service}")
public interface AiMlClient {

    @GetMapping("/api/v1/ai/attrition/risk")
    List<AttritionRiskDto> attritionRisks(@RequestParam(required = false) UUID departmentId);

    @GetMapping("/api/v1/ai/payroll/forecast")
    PayrollForecastDto payrollForecast();
}
