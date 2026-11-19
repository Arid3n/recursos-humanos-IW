package com.iw.rrhh.leave.service;

import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.leave.domain.LeaveRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Único componente de reparto de los días computados de una solicitud entre años naturales (Art. 14.3).
 * Lo usan la validación de saldo al aprobar (HU-05) y el cálculo derivado del saldo (HU-02).
 */
@Component
public class LeaveDayAllocator {

    private final HolidayCalendarService holidayCalendarService;

    public LeaveDayAllocator(HolidayCalendarService holidayCalendarService) {
        this.holidayCalendarService = holidayCalendarService;
    }

    /**
     * Reparte {@code calculatedDays} entre los años naturales del periodo. El total es siempre
     * {@code calculatedDays}: cada año salvo el último recibe {@code min(laborables del tramo hoy, restante)}
     * y el último año recibe el resto, de modo que la suma coincide aunque los festivos hayan cambiado.
     * Un periodo de un solo año devuelve {@code calculatedDays} sin recalcular.
     */
    public Map<Integer, Integer> allocate(LocalDate start, LocalDate end, int calculatedDays) {
        Map<Integer, Integer> allocation = new LinkedHashMap<>();
        int firstYear = start.getYear();
        int lastYear = end.getYear();
        if (firstYear == lastYear) {
            allocation.put(firstYear, calculatedDays);
            return allocation;
        }

        int remaining = calculatedDays;
        for (int year = firstYear; year < lastYear; year++) {
            LocalDate stretchStart = (year == firstYear) ? start : LocalDate.of(year, 1, 1);
            LocalDate stretchEnd = LocalDate.of(year, 12, 31);
            int share = Math.min(holidayCalendarService.countWorkingDays(stretchStart, stretchEnd), remaining);
            allocation.put(year, share);
            remaining -= share;
        }
        allocation.put(lastYear, remaining);
        return allocation;
    }

    /** Días de la solicitud que cuentan en {@code year}; 0 si la solicitud no toca ese año. */
    public int daysInYear(LeaveRequest request, int year) {
        return allocate(request.getStartDate(), request.getEndDate(), request.getCalculatedDays())
                .getOrDefault(year, 0);
    }
}
