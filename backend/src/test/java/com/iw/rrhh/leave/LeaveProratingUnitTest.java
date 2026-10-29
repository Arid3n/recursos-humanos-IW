package com.iw.rrhh.leave;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeaveProratingUnitTest {

    private int calculateProratedDays(int annualDays, int hireMonth) {
        int remainingMonths = 12 - hireMonth + 1;
        return (int) Math.round((annualDays / 12.0) * remainingMonths);
    }

    @Test
    @DisplayName("Incorporación en Enero disfruta del 100% de días de convenio")
    void testJanuaryHireFullDays() {
        assertEquals(22, calculateProratedDays(22, 1));
        assertEquals(24, calculateProratedDays(24, 1));
    }

    @Test
    @DisplayName("Incorporación el 1 de Julio disfruta de exactamente 6 meses prorrateados")
    void testJulyHireHalfYear() {
        // round((22 / 12) * 6) = round(11.0) = 11
        assertEquals(11, calculateProratedDays(22, 7));
        // round((24 / 12) * 6) = 12
        assertEquals(12, calculateProratedDays(24, 7));
    }

    @ParameterizedTest(name = "Mes {0} de incorporación con {1} días anuales resulta en {2} días correspondientes")
    @CsvSource({
        "1, 22, 22",
        "2, 22, 20",
        "3, 22, 18",
        "4, 22, 17",
        "5, 22, 15",
        "6, 22, 13",
        "7, 22, 11",
        "8, 22, 9",
        "9, 22, 7",
        "10, 22, 6",
        "11, 22, 4",
        "12, 22, 2"
    })
    void testAllMonthsProrating(int month, int annualDays, int expectedDays) {
        assertEquals(expectedDays, calculateProratedDays(annualDays, month));
    }
}

