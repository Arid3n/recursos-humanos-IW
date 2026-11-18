package com.iw.rrhh.common.exception;

/**
 * La aprobación de una solicitud no está permitida por una regla de negocio (HU-05):
 * año natural cerrado o saldo insuficiente en alguno de los años que abarca. Se traduce a 422.
 */
public class LeaveApprovalNotAllowedException extends RuntimeException {

    private final int year;
    private final Integer requestedDays;
    private final Integer availableDays;

    /** Año cerrado: no hay días solicitados ni disponibles que informar. */
    public LeaveApprovalNotAllowedException(String detail, int year) {
        this(detail, year, null, null);
    }

    public LeaveApprovalNotAllowedException(String detail, int year, Integer requestedDays, Integer availableDays) {
        super(detail);
        this.year = year;
        this.requestedDays = requestedDays;
        this.availableDays = availableDays;
    }

    public static LeaveApprovalNotAllowedException insufficientBalance(int year, int requestedDays, int availableDays) {
        return new LeaveApprovalNotAllowedException(
                String.format("El saldo de %d no es suficiente: se necesitan %d días y hay %d disponibles",
                        year, requestedDays, availableDays),
                year, requestedDays, availableDays);
    }

    public static LeaveApprovalNotAllowedException closedYear(int year) {
        return new LeaveApprovalNotAllowedException(
                String.format("No se puede aprobar una solicitud con fechas en el año %d, que ya está cerrado", year),
                year);
    }

    public int getYear() {
        return year;
    }

    public Integer getRequestedDays() {
        return requestedDays;
    }

    public Integer getAvailableDays() {
        return availableDays;
    }
}
