package cl.duocuc.rinconsur.auth.exception;

public final class CredencialExistenteException extends RuntimeException {

    public CredencialExistenteException() {
        super("El usuario ya tiene credenciales");
    }
}
