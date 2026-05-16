package kz.aitu.hrms.integration.service.bank;

import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import kz.aitu.hrms.integration.service.CompanySettingSnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface BankFileGenerator {
    String format();

    void write(List<PayslipDetailDto> payslips,
               CompanySettingSnapshot settings,
               OutputStream out) throws IOException;
}
