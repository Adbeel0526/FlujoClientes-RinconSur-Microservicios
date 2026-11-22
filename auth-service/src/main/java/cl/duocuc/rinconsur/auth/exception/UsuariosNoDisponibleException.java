package cl.duocuc.rinconsur.auth.exception;

public final class UsuariosNoDisponibleException extends RuntimeException {

    public UsuariosNoDisponibleException() {
        super("No se pudo consultar el servicio de usuarios");
    }
}
