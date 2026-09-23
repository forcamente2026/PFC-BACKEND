package forcamente.api.config;

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

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Seguranca - rotas publicas, protegidas e corpo dos 401")
class SegurancaRotasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("cadastro continua publico: sem token chega ao controller e recebe 400 de validacao")
    void cadastroEhPublico() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos"));
    }

    @Test
    @DisplayName("rota protegida sem token recebe 401 com message")
    void rotaProtegidaSemTokenRecebe401ComCorpo() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("rota protegida com token invalido recebe 401 com message")
    void rotaProtegidaComTokenInvalidoRecebe401ComCorpo() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("rota protegida com token valido recebe 200")
    void rotaProtegidaComTokenValidoRecebe200() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis").header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("categorias profissionais deixou de ser publica")
    void categoriasProfissionaisExigeToken() throws Exception {
        mockMvc.perform(get("/api/usuarios/categorias-profissionais"))
                .andExpect(status().isUnauthorized());
    }

    private String tokenDeTeste(String papel) {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("forcamente-api")
                .subject(UUID.randomUUID().toString())
                .claim("papel", papel)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    @DisplayName("aluno pode listar exercicios")
    void alunoPodeListarExercicios() throws Exception {
        mockMvc.perform(get("/api/exercicios").header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno nao pode criar exercicio: 403 com message, antes de qualquer validacao")
    void alunoNaoPodeCriarExercicio() throws Exception {
        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    @Test
    @DisplayName("aluno nao pode excluir exercicio, mesmo inexistente: 403 vem antes do 404")
    void alunoNaoPodeExcluirExercicio() throws Exception {
        mockMvc.perform(delete("/api/exercicios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor passa pela autorizacao ao criar exercicio: chega ao controller e recebe 400 de validacao")
    void professorPassaPelaAutorizacaoAoCriar() throws Exception {
        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenDeTeste("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos"));
    }

    @Test
    @DisplayName("documentos legais sao publicos: sem token devolve os dois vigentes")
    void documentosLegaisSaoPublicos() throws Exception {
        mockMvc.perform(get("/api/documentos-legais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versao").isNotEmpty());
    }
}