package kz.aitu.hrms.integration.service.bank;

import org.springframework.stereotype.Component;

/** Jusan Bank salary register — semicolon-separated (RU-locale CSV). */
@Component
public class JusanCsvGenerator extends DelimitedRegisterGenerator {

    @Override
    public String format() {
        return "JUSAN_CSV";
    }

    @Override
    protected char delimiter() {
        return ';';
    }
}
