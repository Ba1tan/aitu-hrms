package kz.aitu.hrms.integration.service.bank;

import org.springframework.stereotype.Component;

/** Kaspi Business salary register — tab-separated. */
@Component
public class KaspiTsvGenerator extends DelimitedRegisterGenerator {

    @Override
    public String format() {
        return "KASPI_TSV";
    }

    @Override
    protected char delimiter() {
        return '\t';
    }
}
