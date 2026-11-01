package com.iw.rrhh.common.exception;

public class LeaveRequestOverlapException extends RuntimeException {

    public LeaveRequestOverlapException(String message) {
        super(message);
    }

    public LeaveRequestOverlapException() {
        super("El periodo seleccionado se solapa con otra solicitud de ausencia previa en estado Pendiente o Aprobada");
    }
}
