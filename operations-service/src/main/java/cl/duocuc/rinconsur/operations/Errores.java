package cl.duocuc.rinconsur.operations;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class Errores {

    @ExceptionHandler(ErrorOperacion.class)
    public ResponseEntity<RespuestaError> negocio(ErrorOperacion error) {
        return respuesta(error.getEstado(), error.getMessage());
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<RespuestaError> solicitud() {
        return respuesta(HttpStatus.BAD_REQUEST, "Revisa los datos ingresados");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> integridad() {
        return respuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes");
    }

    private ResponseEntity<RespuestaError> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado)
                .body(new RespuestaError(estado.value(), mensaje));
    }
}
