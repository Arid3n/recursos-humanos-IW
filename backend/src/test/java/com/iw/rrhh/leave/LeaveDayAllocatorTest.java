package com.iw.rrhh.leave;

import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.service.LeaveDayAllocator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reparto de días de una solicitud entre años naturales (HU-05, aclaración C4). Se usa el calendario real:
 * el 28/12/2026 es lunes; el 01/01/2027 (viernes) y el 06/01/2027 son festivos.
 */
class LeaveDayAllocatorTest {

    private final HolidayCalendarService holidays = new HolidayCalendarService();
    private final LeaveDayAllocator allocator = new LeaveDayAllocator(holidays);

    private static LeaveRequest request(LocalDate start, LocalDate end, int days) {
        return new LeaveRequest(null, LeaveType.VACATION, start, end, days);
    }

    @Test
    @DisplayName("HU-05.CA-5: una solicitud de un solo año devuelve calculatedDays sin recalcular")
    void singleYear_returnsCalculatedDaysWithoutRecalculation() {
        // 99 no coincide con los laborables reales: demuestra que no se recalcula
        Map<Integer, Integer> result = allocator.allocate(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 99);

        assertThat(result).containsExactly(Map.entry(2026, 99));
    }

    @Test
    @DisplayName("HU-05.CA-5: cruce de año reparte laborables al primer año y el resto al último")
    void crossYear_splitsByWorkingDays() {
        // 28/12–31/12/2026 = 4 laborables; 01/01–08/01/2027 = 4 laborables (01 y 06 son festivos)
        Map<Integer, Integer> result = allocator.allocate(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 8), 8);

        assertThat(result).containsExactly(Map.entry(2026, 4), Map.entry(2027, 4));
    }

    @Test
    @DisplayName("HU-05.CA-5: si hoy hay más laborables que calculatedDays, el primer año se limita y el total se conserva")
    void crossYear_todayHasMoreWorkingDays_isCappedAndSumMatches() {
        Map<Integer, Integer> result = allocator.allocate(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 8), 3);

        assertThat(result).containsExactly(Map.entry(2026, 3), Map.entry(2027, 0));
        assertThat(result.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(3);
    }

    @Test
    @DisplayName("HU-05.CA-5: si hoy hay menos laborables que calculatedDays (festivos cambiados), el último año recibe el resto")
    void crossYear_todayHasFewerWorkingDays_lastYearGetsRemainder() {
        Map<Integer, Integer> result = allocator.allocate(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 8), 10);

        assertThat(result).containsExactly(Map.entry(2026, 4), Map.entry(2027, 6));
        assertThat(result.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(10);
    }

    @Test
    @DisplayName("HU-05.CA-5: un periodo de tres años reparte cada año intermedio con sus laborables y el resto al último")
    void threeYears_middleYearUsesItsWorkingDays() {
        int workingDays2027 = holidays.countWorkingDays(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));

        Map<Integer, Integer> result = allocator.allocate(
                LocalDate.of(2026, 12, 28), LocalDate.of(2028, 1, 5), 4 + workingDays2027 + 2);

        assertThat(result).containsExactly(
                Map.entry(2026, 4), Map.entry(2027, workingDays2027), Map.entry(2028, 2));
    }

    @Test
    @DisplayName("HU-05.CA-5: daysInYear devuelve el reparto del año y 0 para un año que la solicitud no toca")
    void daysInYear_returnsShareOrZero() {
        LeaveRequest crossYear = request(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 8), 8);
        LeaveRequest contained = request(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);

        assertThat(allocator.daysInYear(crossYear, 2026)).isEqualTo(4);
        assertThat(allocator.daysInYear(crossYear, 2027)).isEqualTo(4);
        assertThat(allocator.daysInYear(crossYear, 2025)).isZero();
        assertThat(allocator.daysInYear(contained, 2026)).isEqualTo(5);
        assertThat(allocator.daysInYear(contained, 2027)).isZero();
    }
}
