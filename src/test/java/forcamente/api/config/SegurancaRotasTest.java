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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}