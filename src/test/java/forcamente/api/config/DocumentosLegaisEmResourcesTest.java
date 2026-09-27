package forcamente.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Documentos legais em resources - arquivos que o semeador le")
class DocumentosLegaisEmResourcesTest {

    private final JsonMapper jackson = JsonMapper.builder().build();

    @Test
    @DisplayName("termos de uso: JSON valido, com secoes e acentos preservados")
    void termosDeUsoEstaoIntegros() throws Exception {
        JsonNode secoes = ler("documentos/termos-uso-1.0.json");

        assertThat(secoes.isArray()).isTrue();
        assertThat(secoes).isNotEmpty();
        assertThat(secoes.get(0).get("titulo").asText()).contains("serviço");
    }

    @Test
    @DisplayName("politica de privacidade: JSON valido, com secoes e acentos preservados")
    void politicaDePrivacidadeEstaIntegra() throws Exception {
        JsonNode secoes = ler("documentos/politica-privacidade-1.0.json");

        assertThat(secoes.isArray()).isTrue();
        assertThat(secoes).isNotEmpty();
        assertThat(secoes.get(0).get("titulo").asText()).isEqualTo("1. Quem trata seus dados");
    }

    @Test
    @DisplayName("toda secao tem titulo, e o texto nao ficou provisorio")
    void todaSecaoTemTitulo() throws Exception {
        for (String caminho : new String[]{
                "documentos/termos-uso-1.0.json",
                "documentos/politica-privacidade-1.0.json"}) {

            JsonNode secoes = ler(caminho);

            for (JsonNode secao : secoes) {
                assertThat(secao.hasNonNull("titulo"))
                        .as("secao sem titulo em %s", caminho)
                        .isTrue();
            }

            assertThat(secoes.toString()).doesNotContain("Texto a definir");
        }
    }

    private JsonNode ler(String caminho) throws Exception {
        try (InputStream entrada = new ClassPathResource(caminho).getInputStream()) {
            return jackson.readTree(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
