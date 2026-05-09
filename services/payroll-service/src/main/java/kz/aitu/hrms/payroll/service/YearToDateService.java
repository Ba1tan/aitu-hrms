package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.payroll.dto.YtdDtos;
import kz.aitu.hrms.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class YearToDateService {

    private final PayslipRepository payslipRepo;

    @Transactional(readOnly = true)
    public YtdDtos.Response ytd(UUID employeeId, Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        List<Object[]> rows = payslipRepo.getYearToDate(employeeId, y);
        Object[] r = (rows == null || rows.isEmpty()) ? new Object[10] : rows.get(0);
        return YtdDtos.Response.builder()
                .employeeId(employeeId)
                .year(y)
                .totalGross(toBd(r[0]))
                .totalEarned(toBd(r[1]))
                .totalNet(toBd(r[2]))
                .totalOpv(toBd(r[3]))
                .totalVosms(toBd(r[4]))
                .totalIpn(toBd(r[5]))
                .totalSo(toBd(r[6]))
                .totalSn(toBd(r[7]))
                .totalOpvr(toBd(r[8]))
                .payslipsCount(toLong(r[9]))
                .build();
    }

    private static BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        return Long.parseLong(o.toString());
    }
}