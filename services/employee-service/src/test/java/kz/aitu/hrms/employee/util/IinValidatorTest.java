package kz.aitu.hrms.employee.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IinValidatorTest {

    // Checked via the algorithm in CLAUDE.md:
    // 9*1 + 1*2 + 0*3 + 9*4 + 1*5 + 5*6 + 3*7 + 0*8 + 0*9 + 1*10 + 2*11 = 135 → 135 % 11 = 3
    private static final String VALID_IIN = "910915300123";

    @Test
    void acceptsValidIin() {
        assertThat(IinValidator.isValid(VALID_IIN)).isTrue();
    }

    @Test
    void rejectsNull() {
        assertThat(IinValidator.isValid(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "12345",
            "12345678901",    // 11 digits
            "1234567890123",  // 13 digits
            "abcdefghijkl",
            "91091530012a"
    })
    void rejectsMalformed(String iin) {
        assertThat(IinValidator.isValid(iin)).isFalse();
    }

    @Test
    void rejectsIinWithWrongCheckDigit() {
        // Flip the last digit so the check no longer matches.
        String tampered = VALID_IIN.substring(0, 11) + (VALID_IIN.charAt(11) == '9' ? '0' : '9');
        assertThat(IinValidator.isValid(tampered)).isFalse();
    }
}