package cl.duocuc.rinconsur.operations;

import org.springframework.http.HttpStatus;

public final class ErrorOperacion extends RuntimeException {

    private final HttpStatus estado;
    private final String message;

    public ErrorOperacion(HttpStatus estado, String message) {
        super(message);
        this.estado = estado;
        this.message = message;
    }

    public final HttpStatus getEstado() {
        return this.estado;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
