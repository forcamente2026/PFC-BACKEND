package forcamente.api.controller;

import forcamente.api.config.ModoApresentacao;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Modo de apresentacao - o administrador liga e desliga o codigo fixo em tempo de execucao")
class ModoApresentacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private ModoApresentacao modoApresentacao;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @AfterEach
    void desligar() {
        modoApresentacao.definir(false);
    }

    @Test
    @DisplayName("nasce desligado: sem ninguem ligar, o codigo continua sorteado")
    void deveNascerDesligado() throws Exception {
        mockMvc.perform(get("/api/configuracao/modo-apresentacao")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.codigoFixo").value(nullValue()));

        assertThat(modoApresentacao.isAtivo())
                .as("o padrao de fabrica nao pode enfraquecer o segundo fator")
                .isFalse();
    }

    @Test
    @DisplayName("o administrador liga e desliga, e a resposta diz qual e o codigo fixo")
    void deveLigarEDesligar() throws Exception {
        String token = tokenDeAdministrador();

        mockMvc.perform(patch("/api/configuracao/modo-apresentacao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.codigoFixo").value("0000"));

        assertThat(modoApresentacao.isAtivo()).isTrue();

        mockMvc.perform(patch("/api/configuracao/modo-apresentacao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.codigoFixo").value(nullValue()));

        assertThat(modoApresentacao.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("professor e aluno recebem 403 na configuracao, e sem token 401")
    void deveBarrarQuemNaoEAdministrador() throws Exception {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores).isNotEmpty();

        mockMvc.perform(get("/api/configuracao/modo-apresentacao")
                        .header("Authorization", "Bearer "
                                + tokenPara(professores.getFirst().getId(), "PROFESSOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/configuracao/modo-apresentacao")
                        .header("Authorization", "Bearer " + tokenPara(UUID.randomUUID(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/configuracao/modo-apresentacao"))
                .andExpect(status().isUnauthorized());

        assertThat(modoApresentacao.isAtivo())
                .as("nenhuma tentativa barrada pode ter ligado o modo")
                .isFalse();
    }

    @Test
    @DisplayName("corpo sem o campo ativo responde 400")
    void deveRecusarCorpoSemOCampo() throws Exception {
        mockMvc.perform(patch("/api/configuracao/modo-apresentacao")
                        .header("Authorization", "Bearer " + tokenDeAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }

    private String tokenDeAdministrador() {
        return tokenPara(UUID.randomUUID(), "ADMINISTRADOR");
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
