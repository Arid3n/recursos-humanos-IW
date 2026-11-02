package com.iw.rrhh.common.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.Set;

@Service
public class HolidayCalendarService {

    // Festivos fijos nacionales
    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),   // Año Nuevo
            MonthDay.of(1, 6),   // Epifanía del Señor
            MonthDay.of(5, 1),   // Fiesta del Trabajo
            MonthDay.of(8, 15),  // Asunción de la Virgen
            MonthDay.of(10, 12), // Fiesta Nacional de España
            MonthDay.of(11, 1),  // Todos los Santos
            MonthDay.of(12, 6),  // Día de la Constitución
            MonthDay.of(12, 8),  // Inmaculada Concepción
            MonthDay.of(12, 25)  // Natividad del Señor
    );

    public boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    public boolean isHoliday(LocalDate date) {
        // 1. Comprobar festivos fijos
        if (FIXED_HOLIDAYS.contains(MonthDay.of(date.getMonth(), date.getDayOfMonth()))) {
            return true;
        }

        // 2. Comprobar festivos móviles (Viernes Santo y Jueves Santo)
        LocalDate easter = computeEasterSunday(date.getYear());
        LocalDate goodFriday = easter.minusDays(2);
        LocalDate maundyThursday = easter.minusDays(3);

        return date.equals(goodFriday) || date.equals(maundyThursday);
    }

    public boolean isWorkingDay(LocalDate date) {
        return !isWeekend(date) && !isHoliday(date);
    }

    public int countWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }

        int workingDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (isWorkingDay(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    public WorkingDaysBreakdown calculateBreakdown(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }

        int workingDays = 0;
        int weekendDays = 0;
        int holidayDays = 0;
        int calendarDays = 0;

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            calendarDays++;
            if (isWeekend(current)) {
                weekendDays++;
            } else if (isHoliday(current)) {
                holidayDays++;
            } else {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return new WorkingDaysBreakdown(startDate, endDate, workingDays, calendarDays, weekendDays, holidayDays);
    }

    /**
     * Algoritmo de Meeus/Jones/Butcher para calcular el Domingo de Pascua
     */
    private LocalDate computeEasterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    public record WorkingDaysBreakdown(
            LocalDate startDate,
            LocalDate endDate,
            int workingDays,
            int calendarDays,
            int weekendDays,
            int holidayDays
    ) {}
}
