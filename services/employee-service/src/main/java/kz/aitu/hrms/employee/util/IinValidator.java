package kz.aitu.hrms.employee.util;

import java.util.stream.IntStream;

public final class IinValidator {

    private static final int[] W1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] W2 = {3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 2};

    private IinValidator() {}

    public static boolean isValid(String iin) {
        if (iin == null || !iin.matches("\\d{12}")) {
            return false;
        }
        int[] digits = iin.chars().map(c -> c - '0').toArray();
        int check = IntStream.range(0, 11).map(i -> digits[i] * W1[i]).sum() % 11;
        if (check == 10) {
            check = IntStream.range(0, 11).map(i -> digits[i] * W2[i]).sum() % 11;
            if (check == 10) {
                return false;
            }
        }
        return check == digits[11];
    }
}