package forcamente.api.controller;

import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("ArtigoController - JSON igual ao que o front envia e recebe")
class ArtigoControllerTest {

    private static final String FONTE = "https://doi.org/10.1519/JSC.0000000000004526";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Test
    @DisplayName("o professor cria e a resposta traz os dezessete campos do contrato")
    void deveCriarArtigoEDevolverOsNomesDoContrato() throws Exception {
        mockMvc.perform(post("/api/artigos")
                        .header("Authorization", "Bearer " + tokenDeProfessor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(FONTE, resumoDe(260))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Tensao mecanica e hipertrofia"))
                .andExpect(jsonPath("$.categoria.codigo").value("FISIOLOGIA"))
                .andExpect(jsonPath("$.categoria.descricao").value("Fisiologia"))
                .andExpect(jsonPath("$.resumo").exists())
                .andExpect(jsonPath("$.fonte").value(FONTE))
                .andExpect(jsonPath("$.autoria").value("Schoenfeld BJ"))
                .andExpect(jsonPath("$.tipoEstudo").value("Rev. sistematica"))
                .andExpect(jsonPath("$.ano").value(2024))
                .andExpect(jsonPath("$.autorNome").exists())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.motivoRejeicao").value(nullValue()))
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(0))
                .andExpect(jsonPath("$.aprovacoesNecessarias").value(3))
                .andExpect(jsonPath("$.podeEditar").value(true))
                .andExpect(jsonPath("$.podeAprovar").value(false))
                .andExpect(jsonPath("$.professoresQueAprovaram").isEmpty())
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.minutosLeitura").doesNotExist())
                .andExpect(jsonPath("$.conteudo").doesNotExist())
                .andExpect(jsonPath("$.autor").doesNotExist());
    }

    @Test
    @DisplayName("resumo com menos de 250 caracteres responde 400 com a lista de campos")
    void deveRecusarResumoCurtoComAListaDeCampos() throws Exception {
        mockMvc.perform(post("/api/artigos")
                        .header("Authorization", "Bearer " + tokenDeProfessor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(FONTE, resumoDe(100))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos"))
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }

    @Test
    @DisplayName("fonte sem https responde 400, porque viraria XSS no link da tela")
    void deveRecusarFonteQueNaoEHttps() throws Exception {
        mockMvc.perform(post("/api/artigos")
                        .header("Authorization", "Bearer " + tokenDeProfessor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido("http://doi.org/10.1519", resumoDe(260))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }

    @Test
    @DisplayName("fonte com credencial embutida responde 400")
    void deveRecusarFonteComCredencialEmbutida() throws Exception {
        mockMvc.perform(post("/api/artigos")
                        .header("Authorization", "Bearer " + tokenDeProfessor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido("https://doi.org@evil.com/x", resumoDe(260))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("aluno criando artigo recebe 403")
    void deveRecusarAlunoCriandoArtigo() throws Exception {
        mockMvc.perform(post("/api/artigos")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(FONTE, resumoDe(260))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("as categorias saem como codigo e descricao, com acento")
    void deveListarAsCategoriasComCodigoEDescricao() throws Exception {
        mockMvc.perform(get("/api/artigos/categorias")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("FISIOLOGIA"))
                .andExpect(jsonPath("$[0].descricao").value("Fisiologia"))
                .andExpect(jsonPath("$[1].descricao").value("Biomecânica"));
    }

    @Test
    @DisplayName("aluno na fila de pendentes recebe 403")
    void deveRecusarAlunoNaFilaDePendentes() throws Exception {
        mockMvc.perform(get("/api/artigos/pendentes")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("artigo inexistente responde 404")
    void deveResponder404ParaArtigoInexistente() throws Exception {
        mockMvc.perform(get("/api/artigos/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sem token, a listagem responde 401")
    void deveResponder401SemToken() throws Exception {
        mockMvc.perform(get("/api/artigos"))
                .andExpect(status().isUnauthorized());
    }

    private String corpoValido(String fonte, String resumo) {
        return """
                {
                  "titulo": "Tensao mecanica e hipertrofia",
                  "categoria": "FISIOLOGIA",
                  "resumo": "%s",
                  "fonte": "%s",
                  "autoria": "Schoenfeld BJ",
                  "tipoEstudo": "Rev. sistematica",
                  "ano": 2024
                }
                """.formatted(resumo, fonte);
    }

    private String resumoDe(int caracteres) {
        return "Revisao dos mecanismos de sinalizacao mTOR. ".repeat(20)
                .substring(0, caracteres);
    }

    private String tokenDeProfessor() {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores)
                .as("a V3 semeia professores e a V7 os deixa aprovados")
                .isNotEmpty();

        return tokenPara(professores.getFirst().getId(), "PROFESSOR");
    }

    private String tokenDe(String papel) {
        return tokenPara(UUID.randomUUID(), papel);
    }

    private String tokenPara(UUID usuarioId, String papel) {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("forcamente-api")
                .subject(usuarioId.toString())
                .claim("papel", papel)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .build();

        return jwtEncoder.encode(
                        JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
