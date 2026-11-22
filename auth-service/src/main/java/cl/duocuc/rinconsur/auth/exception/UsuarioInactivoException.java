package cl.duocuc.rinconsur.auth.exception;

public final class UsuarioInactivoException extends RuntimeException {

    public UsuarioInactivoException() {
        super("El usuario está inactivo");
    }
}
