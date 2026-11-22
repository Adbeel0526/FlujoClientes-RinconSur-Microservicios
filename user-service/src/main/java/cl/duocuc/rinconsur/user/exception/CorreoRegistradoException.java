package cl.duocuc.rinconsur.user.exception;

public final class CorreoRegistradoException extends RuntimeException {

    public CorreoRegistradoException() {
        super("El correo ya está registrado");
    }
}
