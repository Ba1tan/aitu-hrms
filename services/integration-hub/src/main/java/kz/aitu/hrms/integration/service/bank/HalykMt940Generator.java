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
public class HalykMt940Generator implements BankFileGenerator {

    @Override
    public String format() {
        return "HALYK_MT940";
    }

    @Override
    public void write(List<PayslipDetailDto> payslips,
                      CompanySettingSnapshot settings,
                      OutputStream out) throws IOException {
        // TODO(accounting-team): verify column order and encoding against Halyk bank MT940 spec
        try (var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            for (PayslipDetailDto p : payslips) {
                writer.write(String.join(",",
                        settings.bin(),
                        p.getEmployeeIin() != null ? p.getEmployeeIin() : "",
                        p.getNetSalary() != null ? p.getNetSalary().toPlainString() : "0"));
                writer.write("\n");
            }
        }
    }
}
