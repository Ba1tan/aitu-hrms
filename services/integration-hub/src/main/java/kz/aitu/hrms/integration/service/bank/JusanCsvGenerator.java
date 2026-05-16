package kz.aitu.hrms.integration.service.bank;

import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import kz.aitu.hrms.integration.service.CompanySettingSnapshot;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JusanCsvGenerator implements BankFileGenerator {

    @Override
    public String format() {
        return "JUSAN_CSV";
    }

    @Override
    public void write(List<PayslipDetailDto> payslips,
                      CompanySettingSnapshot settings,
                      OutputStream out) throws IOException {
        // TODO(accounting-team): verify column order and encoding against Jusan bank CSV spec
        try (var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write("BIN,IIN,NetSalary,FullName\n");
            for (PayslipDetailDto p : payslips) {
                writer.write(String.join(",",
                        settings.bin(),
                        p.getEmployeeIin() != null ? p.getEmployeeIin() : "",
                        p.getNetSalary() != null ? p.getNetSalary().toPlainString() : "0",
                        p.getFullName() != null ? p.getFullName() : ""));
                writer.write("\n");
            }
        }
    }
}
