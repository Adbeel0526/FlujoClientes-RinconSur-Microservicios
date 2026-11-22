package cl.duocuc.rinconsur.user.exception;

public final class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException() {
        super("No se encontró el usuario");
    }
}
