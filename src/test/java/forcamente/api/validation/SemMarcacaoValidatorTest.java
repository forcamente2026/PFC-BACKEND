package forcamente.api.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SemMarcacao - barra abertura de tag sem estragar texto legitimo")
class SemMarcacaoValidatorTest {

    private final SemMarcacaoValidator validador = new SemMarcacaoValidator();

    @Test
    @DisplayName("aceita o texto de musculacao que usa maior e menor que")
    void deveAceitarComparacoesNumericas() {
        assertThat(aceita("Qual carga e > 80% de 1RM?")).isTrue();
        assertThat(aceita("Use x < y para a progressao")).isTrue();
        assertThat(aceita("A faixa de 6 < repeticoes < 12")).isTrue();
    }

    @Test
    @DisplayName("aceita apostrofo, acento e ponto e virgula, que nao sao risco de SQL")
    void deveAceitarPontuacaoComum() {
        assertThat(aceita("O papel do 'drive' metabolico")).isTrue();
        assertThat(aceita("Tensão mecânica; volume; frequência")).isTrue();
        assertThat(aceita("O'Brien citou a hipertrofia")).isTrue();
    }

    @Test
    @DisplayName("recusa tudo que abre tag, comentario ou instrucao de processamento")
    void deveRecusarAberturaDeMarcacao() {
        assertThat(aceita("<script>alert(1)</script>")).isFalse();
        assertThat(aceita("Texto antes <img src=x onerror=alert(1)>")).isFalse();
        assertThat(aceita("fim de bloco </div>")).isFalse();
        assertThat(aceita("<!-- comentario escondido -->")).isFalse();
        assertThat(aceita("<?php echo 1; ?>")).isFalse();
    }

    @Test
    @DisplayName("recusa marcacao em qualquer linha, nao so na primeira")
    void deveRecusarMarcacaoEmOutraLinha() {
        assertThat(aceita("Primeira linha legitima\n<script>alert(1)</script>")).isFalse();
    }

    @Test
    @DisplayName("deixa o nulo para o @NotBlank resolver")
    void deveAceitarNulo() {
        assertThat(aceita(null)).isTrue();
    }

    private boolean aceita(String valor) {
        return validador.isValid(valor, null);
    }
}
