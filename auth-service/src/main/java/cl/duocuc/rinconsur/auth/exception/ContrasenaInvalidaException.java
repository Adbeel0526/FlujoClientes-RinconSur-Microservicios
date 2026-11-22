package cl.duocuc.rinconsur.auth.exception;

public final class ContrasenaInvalidaException extends RuntimeException {

    public ContrasenaInvalidaException() {
        super("La contraseña no puede superar los 72 bytes en UTF-8");
    }
}
