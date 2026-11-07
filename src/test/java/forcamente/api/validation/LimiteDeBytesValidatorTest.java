package forcamente.api.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LimiteDeBytes - conta bytes em UTF-8, nao caracteres")
class LimiteDeBytesValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("nulo e aceito, porque quem exige a presenca e o NotBlank")
    void deveAceitarNulo() {
        assertThat(aceita(null)).isTrue();
    }

    @Test
    @DisplayName("o limite exato passa e um byte acima nao")
    void deveAceitarOLimiteExato() {
        assertThat(aceita("x".repeat(72))).isTrue();
        assertThat(aceita("x".repeat(73))).isFalse();
    }

    @Test
    @DisplayName("acento conta dois bytes, entao 37 letras acentuadas ja passam de 72")
    void deveContarOAcentoComoDoisBytes() {
        assertThat(aceita("á".repeat(36))).isTrue();
        assertThat(aceita("á".repeat(37))).isFalse();
    }

    @Test
    @DisplayName("emoji conta quatro bytes, e e por isso que contar caractere nao serve")
    void deveContarOEmojiComoQuatroBytes() {
        assertThat(aceita("🏋".repeat(18))).isTrue();
        assertThat(aceita("🏋".repeat(19))).isFalse();
    }

    private boolean aceita(String valor) {
        return validator.validate(new Alvo(valor)).isEmpty();
    }

    private record Alvo(@LimiteDeBytes(max = 72) String valor) {
    }
}
