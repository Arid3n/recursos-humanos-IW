package com.iw.rrhh.common.exception;

public class InsufficientLeaveBalanceException extends RuntimeException {

    private final int requestedDays;
    private final int availableDays;

    public InsufficientLeaveBalanceException(int requestedDays, int availableDays) {
        super(String.format("El número de días solicitados (%d) supera el saldo disponible (%d)", requestedDays, availableDays));
        this.requestedDays = requestedDays;
        this.availableDays = availableDays;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public int getAvailableDays() {
        return availableDays;
    }
}
