package kz.aitu.hrms.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

public class YtdDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID employeeId;
        private int year;
        private long payslipsCount;

        private BigDecimal totalGross;
        private BigDecimal totalEarned;
        private BigDecimal totalNet;

        private BigDecimal totalOpv;
        private BigDecimal totalVosms;
        private BigDecimal totalIpn;
        private BigDecimal totalSo;
        private BigDecimal totalSn;
        private BigDecimal totalOpvr;
    }
}