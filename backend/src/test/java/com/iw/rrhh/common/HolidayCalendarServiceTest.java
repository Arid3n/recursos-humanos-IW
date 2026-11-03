package com.iw.rrhh.common;

import com.iw.rrhh.common.service.HolidayCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HolidayCalendarServiceTest {

    private HolidayCalendarService holidayCalendarService;

    @BeforeEach
    void setUp() {
        holidayCalendarService = new HolidayCalendarService();
    }

    @Test
    @DisplayName("Debe detectar correctamente sábados y domingos como fin de semana")
    void testWeekendDetection() {
        LocalDate saturday = LocalDate.of(2026, 7, 11);
        LocalDate sunday = LocalDate.of(2026, 7, 12);
        LocalDate monday = LocalDate.of(2026, 7, 13);

        assertThat(holidayCalendarService.isWeekend(saturday)).isTrue();
        assertThat(holidayCalendarService.isWeekend(sunday)).isTrue();
        assertThat(holidayCalendarService.isWeekend(monday)).isFalse();
    }

    @Test
    @DisplayName("Debe detectar festivos oficiales fijos y móviles")
    void testHolidayDetection() {
        // Festivos fijos
        LocalDate newYear = LocalDate.of(2026, 1, 1);
        LocalDate laborDay = LocalDate.of(2026, 5, 1);
        LocalDate christmas = LocalDate.of(2026, 12, 25);
        LocalDate normalDay = LocalDate.of(2026, 7, 7);

        assertThat(holidayCalendarService.isHoliday(newYear)).isTrue();
        assertThat(holidayCalendarService.isHoliday(laborDay)).isTrue();
        assertThat(holidayCalendarService.isHoliday(christmas)).isTrue();
        assertThat(holidayCalendarService.isHoliday(normalDay)).isFalse();

        // Viernes Santo 2026 (Pascua es 5 de abril, Viernes Santo es 3 de abril)
        LocalDate goodFriday2026 = LocalDate.of(2026, 4, 3);
        assertThat(holidayCalendarService.isHoliday(goodFriday2026)).isTrue();
    }

    @Test
    @DisplayName("Debe computar 5 días laborables en una semana ordinaria de lunes a viernes")
    void testCountWorkingDays_NormalWeek() {
        LocalDate monday = LocalDate.of(2026, 7, 6);
        LocalDate friday = LocalDate.of(2026, 7, 10);

        int workingDays = holidayCalendarService.countWorkingDays(monday, friday);

        assertThat(workingDays).isEqualTo(5);
    }

    @Test
    @DisplayName("Debe descontar festivos en el cómputo de días laborables")
    void testCountWorkingDays_WithHoliday() {
        // Del lunes 27 de abril de 2026 al viernes 1 de mayo (1 de mayo es festivo)
        LocalDate monday = LocalDate.of(2026, 4, 27);
        LocalDate friday = LocalDate.of(2026, 5, 1);

        int workingDays = holidayCalendarService.countWorkingDays(monday, friday);

        assertThat(workingDays).isEqualTo(4);
    }

    @Test
    @DisplayName("Debe devolver 0 días laborables para un fin de semana")
    void testCountWorkingDays_WeekendOnly() {
        LocalDate saturday = LocalDate.of(2026, 7, 11);
        LocalDate sunday = LocalDate.of(2026, 7, 12);

        int workingDays = holidayCalendarService.countWorkingDays(saturday, sunday);

        assertThat(workingDays).isZero();
    }

    @Test
    @DisplayName("Debe desglosar correctamente días naturales, laborables, fin de semana y festivos")
    void testBreakdownCalculation() {
        // Semana completa: lunes 27 de abril al domingo 3 de mayo (7 días naturales)
        // 4 laborables (lunes a jueves), 1 festivo (viernes 1 de mayo), 2 fin de semana (sábado y domingo)
        LocalDate start = LocalDate.of(2026, 4, 27);
        LocalDate end = LocalDate.of(2026, 5, 3);

        HolidayCalendarService.WorkingDaysBreakdown breakdown = holidayCalendarService.calculateBreakdown(start, end);

        assertThat(breakdown.calendarDays()).isEqualTo(7);
        assertThat(breakdown.workingDays()).isEqualTo(4);
        assertThat(breakdown.holidayDays()).isEqualTo(1);
        assertThat(breakdown.weekendDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("HU-06.CA-3: Debe devolver el nombre de un festivo fijo")
    void testGetHolidayName_FixedHoliday() {
        assertThat(holidayCalendarService.getHolidayName(LocalDate.of(2026, 12, 8)))
                .contains("Inmaculada Concepción");
        assertThat(holidayCalendarService.getHolidayName(LocalDate.of(2026, 1, 1)))
                .contains("Año Nuevo");
    }

    @Test
    @DisplayName("HU-06.CA-3: Debe devolver el nombre de los festivos móviles")
    void testGetHolidayName_MovableHolidays() {
        // Pascua 2026: 5 de abril
        assertThat(holidayCalendarService.getHolidayName(LocalDate.of(2026, 4, 2)))
                .contains("Jueves Santo");
        assertThat(holidayCalendarService.getHolidayName(LocalDate.of(2026, 4, 3)))
                .contains("Viernes Santo");
    }

    @Test
    @DisplayName("HU-06.CA-3: Debe devolver vacío para un día que no es festivo")
    void testGetHolidayName_NotHoliday() {
        assertThat(holidayCalendarService.getHolidayName(LocalDate.of(2026, 7, 7))).isEmpty();
    }

    @Test
    @DisplayName("Debe lanzar excepción si endDate es anterior a startDate")
    void testInvalidDateRange() {
        LocalDate start = LocalDate.of(2026, 7, 10);
        LocalDate end = LocalDate.of(2026, 7, 5);

        assertThatThrownBy(() -> holidayCalendarService.countWorkingDays(start, end))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
