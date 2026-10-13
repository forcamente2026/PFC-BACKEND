package forcamente.api.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MetricasController - JSON igual ao que o front envia")
class MetricasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("gasto energetico aceita pesoKg e devolve TMB e gasto total")
    void gastoEnergeticoComJsonDoFront() throws Exception {
        mockMvc.perform(post("/api/metricas/gasto-energetico")
                        .header("Authorization", "Bearer " + tokenDeTeste())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80,\"alturaCm\":180,\"idade\":30,\"sexoBiologico\":\"MASCULINO\",\"nivelAtividade\":\"MODERADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmb").value(1780))
                .andExpect(jsonPath("$.gastoEnergeticoTotal").value(2759));
    }

    @Test
    @DisplayName("hidratacao aceita pesoKg e minutosTreinoPorDia")
    void hidratacaoComJsonDoFront() throws Exception {
        mockMvc.perform(post("/api/metricas/hidratacao")
                        .header("Authorization", "Bearer " + tokenDeTeste())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":72.5,\"minutosTreinoPorDia\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mlPorDia").value(3038))
                .andExpect(jsonPath("$.adicionalTreinoMl").value(500));
    }

    private String tokenDeTeste() {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("forcamente-api")
                .subject(UUID.randomUUID().toString())
                .claim("papel", "ALUNO")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}