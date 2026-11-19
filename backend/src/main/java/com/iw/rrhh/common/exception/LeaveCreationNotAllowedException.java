package com.iw.rrhh.common.exception;

/**
 * El usuario no puede crear solicitudes de ausencia (HU-05 FR-015 y FR-017): no tiene un
 * resolutor válido o es de Dirección General. Se traduce a 422.
 */
public class LeaveCreationNotAllowedException extends RuntimeException {

    public LeaveCreationNotAllowedException(String message) {
        super(message);
    }
}
