package cl.duocuc.rinconsur.auth.exception;

public final class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException() {
        super("No se encontró el usuario");
    }
}
