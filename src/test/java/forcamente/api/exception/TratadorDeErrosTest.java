package forcamente.api.exception;





import forcamente.api.controller.UsuarioController;
import forcamente.api.dto.UsuarioRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TratadorDeErros - status e corpo de cada tipo de erro")
class TratadorDeErrosTest {

    private final TratadorDeErros tratador = new TratadorDeErros();

    @Test
    @DisplayName("recurso nao encontrado vira 404 com messagem e campo vazio")
    void deveResponder404() {
        var resposta = tratador.tratarNaoEncontrado(new RecursoNaoEncontradoException("Usuario nao encontrado"));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().status()).isEqualTo(404);
        assertThat(resposta.getBody().message()).isEqualTo("Usuario nao encontrado");
        assertThat(resposta.getBody().campos()).isEmpty();


    }
    @Test
    @DisplayName("conflito vira 409")
    void deveResponder409() {
        var resposta = tratador.tratarConflito(new ConflitoException("Ja existe"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().message()).isEqualTo("Ja existe");
    }

    @Test
    @DisplayName("regra de negocio vira 400 e credenciais invalidas vira 401")
    void deveResponder400E401() {
        assertThat(tratador.tratarRegraDeNegocio(new RegraDeNegocioException("x")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tratador.tratarCredenciaisInvalidas(new CredenciaisInvalidasException("x")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("validacao vira 400 com a lista de campos")
    void deveResponder400ComCampos() throws NoSuchMethodException {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "usuarioRequestDTO");
        bindingResult.addError(new FieldError("usuarioRequestDTO", "cpf", "O CPF deve conter 11 digitos"));
        bindingResult.addError(new FieldError("usuarioRequestDTO", "senha", "A senha e obrigatoria"));
        var parametro = new MethodParameter(
                UsuarioController.class.getMethod("criarUsuario", UsuarioRequestDTO.class), 0);
        var excecao = new MethodArgumentNotValidException(parametro, bindingResult);

        var resposta = tratador.tratarValidacao(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).isEqualTo("Dados invalidos");
        assertThat(resposta.getBody().campos()).hasSize(2);
        assertThat(resposta.getBody().campos().getFirst().campo()).isEqualTo("cpf");
        assertThat(resposta.getBody().campos().getFirst().mensagem()).isEqualTo("O CPF deve conter 11 digitos");
    }


    @Test
    @DisplayName("nao autenticado vira 401 e acesso negado vira 403, ambos com message fixa")
    void deveResponder401E403DoSecurity() {
        var naoAutenticado = tratador.tratarNaoAutenticado(
                new org.springframework.security.authentication.BadCredentialsException("detalhe-interno-do-spring"));
        var negado = tratador.tratarAcessoNegado(
                new org.springframework.security.access.AccessDeniedException("detalhe-interno-do-spring"));

        assertThat(naoAutenticado.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(naoAutenticado.getBody().message()).doesNotContain("detalhe-interno-do-spring");
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(negado.getBody().message()).doesNotContain("detalhe-interno-do-spring");
    }
}


