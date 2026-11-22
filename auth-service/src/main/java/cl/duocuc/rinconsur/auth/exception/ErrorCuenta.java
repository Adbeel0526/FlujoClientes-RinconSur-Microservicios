package cl.duocuc.rinconsur.auth.exception;

import org.springframework.http.HttpStatus;

public final class ErrorCuenta extends RuntimeException {

    private final HttpStatus estado;
    private final String message;

    public ErrorCuenta(HttpStatus estado, String message) {
        super(message);
        this.estado = estado;
        this.message = message;
    }

    public final HttpStatus getEstado() {
        return this.estado;
    }

    public String getMessage() {
        return this.message;
    }
}
