package kz.aitu.hrms.integration.service.bank;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Renders a salary payment register for one payroll period in a specific bank's
 * upload format. The accountant downloads the file and uploads it to the bank's
 * business portal ("зарплатный проект" → "загрузить реестр") — there is no live
 * bank API.
 *
 * <p>Exact column order / encoding still differs per bank and must be validated
 * against the bank's current template; these implementations produce the
 * canonical KZ salary-register columns (IBAN, IIN, name, amount, КНП, КБе).
 */
public interface BankFileGenerator {

    /** Format key stored in the {@code integration.bank_default_format} setting. */
    String format();

    void write(List<BankPaymentRow> rows, BankFileContext ctx, OutputStream out) throws IOException;
}
