package kz.aitu.hrms.integration.service.bank;

import org.springframework.stereotype.Component;

/**
 * Halyk Bank salary register — comma-separated.
 *
 * (Replaces the earlier "HALYK_MT940" stub: MT940 is a bank <em>statement</em>
 * format, not a salary payout register. Halyk's salary project accepts an
 * uploaded XLS/CSV/XML registry whose interbank transfer runs as MT-102.)
 */
@Component
public class HalykCsvGenerator extends DelimitedRegisterGenerator {

    @Override
    public String format() {
        return "HALYK_CSV";
    }

    @Override
    protected char delimiter() {
        return ',';
    }
}
