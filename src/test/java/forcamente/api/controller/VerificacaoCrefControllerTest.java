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
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Verificacao do CREF - rotas do administrador e renovacao")
class VerificacaoCrefControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Test
    @DisplayName("a fila de verificacao devolve os seis campos do contrato")
    void deveDevolverOsCamposDaFilaDeVerificacao() throws Exception {
        mockMvc.perform(get("/api/usuarios/pendentes-verificacao")
                        .header("Authorization", "Bearer " + tokenDe("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("aluno na fila de verificacao recebe 403")
    void deveRecusarAlunoNaFilaDeVerificacao() throws Exception {
        mockMvc.perform(get("/api/usuarios/pendentes-verificacao")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor decidindo verificacao recebe 403: a decisao e so do administrador")
    void deveRecusarProfessorDecidindoVerificacao() throws Exception {
        mockMvc.perform(patch("/api/usuarios/" + UUID.randomUUID() + "/verificacao")
                        .header("Authorization", "Bearer " + tokenDe("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APROVADO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rejeitar sem motivo responde 400 com a lista de campos")
    void deveRecusarRejeicaoSemMotivo() throws Exception {
        mockMvc.perform(patch("/api/usuarios/" + idDeProfessorAprovado() + "/verificacao")
                        .header("Authorization", "Bearer " + tokenDe("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJEITADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }

    @Test
    @DisplayName("decidir com situacao que nao e aprovado nem rejeitado responde 400")
    void deveRecusarSituacaoQueNaoEDecidivel() throws Exception {
        mockMvc.perform(patch("/api/usuarios/" + idDeProfessorAprovado() + "/verificacao")
                        .header("Authorization", "Bearer " + tokenDe("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDENTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("decidir a verificacao de um professor ja aprovado responde 409")
    void deveResponder409AoDecidirVerificacaoJaAprovada() throws Exception {
        mockMvc.perform(patch("/api/usuarios/" + idDeProfessorAprovado() + "/verificacao")
                        .header("Authorization", "Bearer " + tokenDe("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APROVADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("minha conta traz a situacao do CREF do professor")
    void deveTrazerASituacaoDoCrefEmMinhaConta() throws Exception {
        var professorId = idDeProfessorAprovado();

        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + tokenPara(professorId, "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoCref").value("APROVADO"))
                .andExpect(jsonPath("$.cref").exists());
    }

    @Test
    @DisplayName("renovacao com CREF fora do formato responde 400")
    void deveRecusarRenovacaoComCrefForaDoFormato() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me/cref")
                        .header("Authorization", "Bearer " + tokenPara(idDeProfessorAprovado(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cref\":\"123\",\"formacao\":\"BACHARELADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }

    @Test
    @DisplayName("renovacao devolve a verificacao para pendente e grava o CREF novo")
    void deveRenovarOCrefEVoltarParaPendente() throws Exception {
        var crefNovo = crefLivre();

        mockMvc.perform(patch("/api/usuarios/me/cref")
                        .header("Authorization", "Bearer " + tokenPara(idDeProfessorAprovado(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRenovacao(crefNovo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cref").value(crefNovo))
                .andExpect(jsonPath("$.situacaoCref").value("PENDENTE"));
    }

    @Test
    @DisplayName("aluno renovando CREF responde 400: nao tem CREF para renovar")
    void deveRecusarAlunoRenovandoCref() throws Exception {
        var aluno = usuarioRepository.findByEmail("ana.ramos@exemplo.test");

        assertThat(aluno).as("a V3 semeia esta conta").isPresent();

        mockMvc.perform(patch("/api/usuarios/me/cref")
                        .header("Authorization", "Bearer " + tokenPara(aluno.get().getId(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRenovacao(crefLivre())))
                .andExpect(status().isBadRequest());
    }

    private String corpoDeRenovacao(String cref) {
        return "{\"cref\":\"%s\",\"formacao\":\"BACHARELADO\"}".formatted(cref);
    }

    private String crefLivre() {
        var candidato = "%06d-G/SP".formatted(ThreadLocalRandom.current().nextInt(200000, 999999));

        assertThat(usuarioRepository.existsByCref(candidato))
                .as("o CREF sorteado para a renovacao precisa estar livre no banco")
                .isFalse();

        return candidato;
    }

    private UUID idDeProfessorAprovado() {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores)
                .as("a V3 semeia professores e a V7 os deixa aprovados")
                .isNotEmpty();

        return professores.getFirst().getId();
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
