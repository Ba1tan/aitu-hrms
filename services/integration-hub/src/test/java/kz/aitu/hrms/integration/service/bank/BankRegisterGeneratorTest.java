package kz.aitu.hrms.integration.service.bank;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BankRegisterGeneratorTest {

    private static final BankFileContext CTX = new BankFileContext(
            "123456789012", "ТОО Тест", "010", "19", "Заработная плата за 03.2026", 2026, 3);

    private static final List<BankPaymentRow> ROWS = List.of(
            new BankPaymentRow("KZ75125KZT2069100100", "900101300700",
                    "Иванов Иван", new BigDecimal("250000.00"), "Kaspi"),
            new BankPaymentRow(null, "900202300800",
                    "Петров Петр", new BigDecimal("180000.5"), null));

    private String render(BankFileGenerator gen) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gen.write(ROWS, CTX, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void kaspi_tabDelimited_withHeaderIbanAmountAndTotal() throws IOException {
        String s = render(new KaspiTsvGenerator());

        assertThat(s).contains("IBAN").contains("ИИН").contains("Сумма").contains("КНП").contains("КБе");
        assertThat(s).contains("KZ75125KZT2069100100");
        assertThat(s).contains("250000.00");
        assertThat(s).contains("\t");                 // tab-separated
        assertThat(s).contains("010").contains("19"); // КНП / КБе from context
        assertThat(s).contains("ИТОГО").contains("430000.50"); // 250000.00 + 180000.50
    }

    @Test
    void halyk_commaDelimited() throws IOException {
        String s = render(new HalykCsvGenerator());
        assertThat(new HalykCsvGenerator().format()).isEqualTo("HALYK_CSV");
        // header line uses commas, not tabs
        assertThat(s.lines().findFirst().orElse("")).contains(",").doesNotContain("\t");
        assertThat(s).contains("KZ75125KZT2069100100");
    }

    @Test
    void jusan_semicolonDelimited() throws IOException {
        String s = render(new JusanCsvGenerator());
        assertThat(new JusanCsvGenerator().format()).isEqualTo("JUSAN_CSV");
        assertThat(s.lines().findFirst().orElse("")).contains(";");
        assertThat(s).contains("180000.50"); // amount rounded to 2dp
    }

    @Test
    void missingIban_rendersBlankCellNotError() throws IOException {
        String s = render(new HalykCsvGenerator());
        // Second beneficiary has no IBAN — still appears with name, no exception.
        assertThat(s).contains("Петров Петр");
    }
}
