package cl.duocuc.rinconsur.user.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class UsuarioExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos() {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("La solicitud contiene datos inválidos", Map.of()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> usuarioNoEncontrado() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("No se encontró el usuario", Map.of()));
    }

    @ExceptionHandler({CorreoRegistradoException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> conflictoDeDatos() {
        String mensaje = "El correo ya está registrado o los datos entran en conflicto";
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(mensaje, Map.of()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> modificacionSimultanea() {
        String mensaje = "El usuario fue modificado por otra solicitud. Vuelve a consultarlo";
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(mensaje, Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(MethodArgumentNotValidException error) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : error.getBindingResult().getFieldErrors()) {
            String nombreCampo = campo.getField();
            String mensaje = Objects.requireNonNullElse(campo.getDefaultMessage(), "Valor inválido");
            errores.put(nombreCampo, mensaje);
        }
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("Revisa los datos ingresados", errores));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> solicitudInvalida() {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("La solicitud contiene datos inválidos", Map.of()));
    }
}
