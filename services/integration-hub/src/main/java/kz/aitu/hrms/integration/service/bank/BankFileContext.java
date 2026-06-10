package kz.aitu.hrms.integration.service.bank;

/**
 * Payer-side context shared by every row of a salary register.
 *
 * @param companyBin  payer company BIN (12 digits)
 * @param companyName payer legal name
 * @param knp         КНП — payment purpose code (KZ classifier; salary, configurable)
 * @param kbe         КБе — beneficiary code (e.g. 19 for resident individuals)
 * @param purpose     free-text payment purpose ("Заработная плата за MM.YYYY")
 * @param year        payroll period year
 * @param month       payroll period month (1–12)
 */
public record BankFileContext(
        String companyBin,
        String companyName,
        String knp,
        String kbe,
        String purpose,
        int year,
        int month) {
}
