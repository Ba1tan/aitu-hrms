package kz.aitu.hrms.integration.service.bank;

import java.math.BigDecimal;

/**
 * One beneficiary line in a salary register: who gets paid, how much, and to
 * which account.
 *
 * @param iban     KZ IBAN of the employee (may be blank if not on file)
 * @param iin      employee IIN (12 digits)
 * @param fullName beneficiary full name
 * @param amount   net amount to credit
 * @param bankName free-text bank name hint (Kaspi / Halyk / Jusan)
 */
public record BankPaymentRow(
        String iban,
        String iin,
        String fullName,
        BigDecimal amount,
        String bankName) {
}
