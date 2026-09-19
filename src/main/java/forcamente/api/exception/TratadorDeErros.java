package forcamente.api.exception;


import forcamente.api.dto.ApiErroDTO;
import forcamente.api.dto.CampoErroDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErroDTO> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiErroDTO> tratarConflito(ConflitoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ApiErroDTO> tratarRegraDeNegocio(RegraDeNegocioException ex){
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ApiErroDTO> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return responder (HttpStatus.UNAUTHORIZED, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErroDTO> tratarValidacao(MethodArgumentNotValidException ex) {
        List<CampoErroDTO> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoErroDTO(erro.getField(), erro.getDefaultMessage())).toList();
        return responder(HttpStatus.BAD_REQUEST, "Dados invalidos", campos);

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErroDTO> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido ou com valor nao reconhecido", List.of());
    }

    private ResponseEntity<ApiErroDTO> responder(HttpStatus status, String message,
                                                 List<CampoErroDTO> campos) {
        return ResponseEntity.status(status).body(new ApiErroDTO(status.value(), message, campos));
    }
}
