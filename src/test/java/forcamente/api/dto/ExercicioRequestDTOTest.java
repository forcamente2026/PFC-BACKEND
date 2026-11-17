package forcamente.api.dto;

import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExercicioRequestDTO - textos sem marcacao HTML e GIF so por link https")
class ExercicioRequestDTOTest {

    private static final String GIF_VALIDO = "https://media.exemplo.com/agachamento.gif";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("aceita GIF https e os sinais < e > soltos, que sao conteudo legitimo neste dominio")
    void deveAceitarDadosValidos() {
        var dto = exercicio("Agachamento livre", "Mantenha 6 < repeticoes < 12 na serie.",
                "Carga > 80% de 1RM sem aquecimento.", "Duas series leves antes.", "Barra e anilhas",
                GIF_VALIDO);

        assertThat(camposInvalidos(dto)).isEmpty();
    }

    @Test
    @DisplayName("GIF vazio ou so com espacos vira ausente, e nao invalido, porque o formulario manda texto vazio")
    void deveTratarGifVazioComoAusente() {
        assertThat(exercicioComGif("").gifUrl()).isNull();
        assertThat(exercicioComGif("   ").gifUrl()).isNull();
        assertThat(camposInvalidos(exercicioComGif(""))).isEmpty();
    }

    @Test
    @DisplayName("GIF com espacos em volta e aparado antes da validacao")
    void deveApararOGif() {
        var dto = exercicioComGif("  " + GIF_VALIDO + "  ");

        assertThat(dto.gifUrl()).isEqualTo(GIF_VALIDO);
        assertThat(camposInvalidos(dto)).isEmpty();
    }

    @Test
    @DisplayName("recusa GIF que nao e link https: javascript, data, http, texto solto, aspas, espacos e marcacao")
    void deveRecusarGifForaDoFormato() {
        var recusados = List.of(
                "javascript:alert(1)",
                "data:image/svg+xml,<svg onload=alert(1)>",
                "http://media.exemplo.com/agachamento.gif",
                "media.exemplo.com/agachamento.gif",
                "https://media.exemplo.com/a.gif\"><script>alert(1)</script>",
                "https://media.exemplo.com/a.gif' onerror='alert(1)",
                "https://media.exemplo.com/meu gif.gif",
                "<script>alert(1)</script>");

        for (String gif : recusados) {
            assertThat(camposInvalidos(exercicioComGif(gif))).as(gif).contains("gifUrl");
        }
    }

    @Test
    @DisplayName("recusa marcacao HTML nos cinco campos de texto do exercicio")
    void deveRecusarMarcacaoNosCamposDeTexto() {
        var dto = exercicio("<script>alert(1)</script>", "<img src=x onerror=alert(1)>",
                "</div>", "<!-- comentario -->", "<b>Barra</b>", GIF_VALIDO);

        assertThat(camposInvalidos(dto)).containsExactlyInAnyOrder(
                "nome", "descricaoExecucao", "errosComuns", "aquecimentoRecomendado", "equipamento");
    }

    private ExercicioRequestDTO exercicioComGif(String gifUrl) {
        return exercicio("Agachamento livre", "Desca ate a paralela e suba.", null, null, null, gifUrl);
    }

    private ExercicioRequestDTO exercicio(String nome, String descricao, String errosComuns,
                                          String aquecimento, String equipamento, String gifUrl) {
        return new ExercicioRequestDTO(nome, GrupoMuscularEnum.PERNA, NivelDificuldadeEnum.INICIANTE,
                descricao, errosComuns, aquecimento, equipamento, gifUrl, null);
    }

    private Set<String> camposInvalidos(ExercicioRequestDTO dto) {
        return validator.validate(dto).stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
