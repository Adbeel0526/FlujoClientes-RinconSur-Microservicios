package cl.duocuc.rinconsur.auth.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(ErrorCuenta.class)
    public ResponseEntity<ErrorResponse> cuenta(ErrorCuenta error) {
        return ResponseEntity.status(error.getEstado()).body(new ErrorResponse(error.getMessage(), Map.of()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> credencialesInvalidas() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Correo o contraseña incorrectos", Map.of()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> usuarioNoEncontrado() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("No se encontró el usuario", Map.of()));
    }

    @ExceptionHandler({CredencialExistenteException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> credencialExistente() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("El usuario ya tiene credenciales", Map.of()));
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ErrorResponse> usuarioInactivo() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("El usuario está inactivo", Map.of()));
    }

    @ExceptionHandler(UsuariosNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> usuariosNoDisponible() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("El servicio de usuarios no está disponible. Intenta nuevamente",
                        Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(MethodArgumentNotValidException error) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : error.getBindingResult().getFieldErrors()) {
            errores.put(campo.getField(), Objects.requireNonNullElse(campo.getDefaultMessage(), "Valor inválido"));
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("Revisa los datos ingresados", errores));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ContrasenaInvalidaException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida() {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("La solicitud contiene datos inválidos", Map.of()));
    }
}
