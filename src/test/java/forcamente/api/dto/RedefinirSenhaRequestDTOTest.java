package forcamente.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedefinirSenhaRequestDTO - codigo e regras da nova senha")
class RedefinirSenhaRequestDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("deve aceitar codigo de 4 digitos e senha forte, normalizando o e-mail")
    void deveAceitarDadosValidos() {
        var dto = new RedefinirSenhaRequestDTO(" Joao@UMC.br ", " 1234 ", "NovaSenha@1");

        assertThat(dto.email()).isEqualTo("joao@umc.br");
        assertThat(dto.codigo()).isEqualTo("1234");
        assertThat(validar(dto)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar senha fraca, com a mesma regra do cadastro")
    void deveRecusarSenhaFraca() {
        var dto = new RedefinirSenhaRequestDTO("joao@umc.br", "1234", "senhafraca");

        assertThat(validar(dto)).contains("novaSenha");
    }

    @Test
    @DisplayName("deve recusar codigo que nao tenha 4 digitos")
    void deveRecusarCodigoInvalido() {
        var dto = new RedefinirSenhaRequestDTO("joao@umc.br", "12a", "NovaSenha@1");

        assertThat(validar(dto)).contains("codigo");
    }

    private Set<String> validar(RedefinirSenhaRequestDTO dto) {
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}