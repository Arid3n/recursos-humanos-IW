package com.iw.rrhh.common.exception;

/**
 * Se lanza al intentar resolver una solicitud que ya no está en estado PENDING (Art. 13.2).
 */
public class LeaveRequestAlreadyResolvedException extends RuntimeException {
    public LeaveRequestAlreadyResolvedException() {
        super("La solicitud ya ha sido resuelta y no puede volver a resolverse");
    }
}
