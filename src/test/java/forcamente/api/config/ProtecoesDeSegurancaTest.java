package forcamente.api.config;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IRegistroAuditoriaRepository;
import forcamente.api.repository.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Protecoes de seguranca - teto de pagina, revogacao de token, justificativa e cabecalhos")
class ProtecoesDeSegurancaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRegistroAuditoriaRepository registroAuditoriaRepository;

    @Test
    @DisplayName("tamanho de pagina acima do teto recebe 400, senao a tabela inteira sobe para a memoria")
    void deveRecusarTamanhoDePaginaAcimaDoTeto() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .param("tamanho", "2000000000")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos"))
                .andExpect(jsonPath("$.campos[0].campo").value("tamanho"));
    }

    @Test
    @DisplayName("tamanho de pagina zerado recebe 400, e nao 500 de dentro do PageRequest")
    void deveRecusarTamanhoDePaginaZerado() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .param("tamanho", "0")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a trilha tambem tem teto de pagina")
    void deveRecusarTamanhoDePaginaNaTrilha() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .param("tamanho", "500")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("token emitido antes da revogacao recebe 401, mesmo com assinatura e validade boas")
    void deveRecusarTokenAnteriorARevogacao() throws Exception {
        UsuarioEntity professor = umProfessorAtivo();

        String token = tokenPara(professor.getId(), PapelUsuarioEnum.PROFESSOR.name(),
                Instant.now().minusSeconds(60));

        professor.setTokenValidoApos(LocalDateTime.now());
        usuarioRepository.save(professor);

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sessao encerrada. Entre novamente."));
    }

    @Test
    @DisplayName("token emitido depois da revogacao continua valendo, senao entrar de novo nao resolveria")
    void deveAceitarTokenPosteriorARevogacao() throws Exception {
        UsuarioEntity professor = umProfessorAtivo();

        professor.setTokenValidoApos(LocalDateTime.now().minusMinutes(5));
        usuarioRepository.save(professor);

        String token = tokenPara(professor.getId(), PapelUsuarioEnum.PROFESSOR.name(), Instant.now());

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("conta inativa tem o token recusado, senao desativar nao cortaria o acesso de quem ja estava dentro")
    void deveRecusarTokenDeContaInativa() throws Exception {
        UsuarioEntity professor = umProfessorAtivo();

        String token = tokenPara(professor.getId(), PapelUsuarioEnum.PROFESSOR.name(), Instant.now());

        professor.setAtivo(false);
        usuarioRepository.save(professor);

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("dados completos sem justificativa recebem 400")
    void deveExigirJustificativaNosDadosCompletos() throws Exception {
        mockMvc.perform(get("/api/usuarios/" + umProfessorAtivo().getId() + "/dados-completos")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("justificativa curta recebe 400, para a trilha nao guardar um ponto como motivo")
    void deveRecusarJustificativaCurta() throws Exception {
        mockMvc.perform(get("/api/usuarios/" + umProfessorAtivo().getId() + "/dados-completos")
                        .param("justificativa", "ok")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a justificativa informada fica gravada no registro da trilha")
    void deveGravarAJustificativaNaTrilha() throws Exception {
        mockMvc.perform(get("/api/usuarios/" + umProfessorAtivo().getId() + "/dados-completos")
                        .param("justificativa", "Conferencia do cadastro pedida pelo titular")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk());

        assertThat(registroAuditoriaRepository.findAll())
                .filteredOn(registro -> registro.getAcao() == AcaoAuditoriaEnum.DADO_COMPLETO_LIDO)
                .extracting(registro -> registro.getJustificativa())
                .contains("Conferencia do cadastro pedida pelo titular");
    }

    @Test
    @DisplayName("as respostas levam os cabecalhos de seguranca, inclusive nas publicas")
    void deveEnviarOsCabecalhosDeSeguranca() throws Exception {
        mockMvc.perform(get("/api/usuarios/formacoes"))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()"));
    }

    private UsuarioEntity umProfessorAtivo() {
        return usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                        PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO)
                .stream()
                .filter(UsuarioEntity::isAtivo)
                .findFirst()
                .orElseThrow();
    }

    private String tokenDeAdministrador() {
        return tokenPara(UUID.randomUUID(), PapelUsuarioEnum.ADMINISTRADOR.name(), Instant.now());
    }

    private String tokenPara(UUID usuarioId, String papel, Instant emitidoEm) {
        var claims = JwtClaimsSet.builder()
                .issuer("forcamente-api")
                .subject(usuarioId.toString())
                .claim("papel", papel)
                .issuedAt(emitidoEm)
                .expiresAt(emitidoEm.plusSeconds(3600))
                .build();

        return jwtEncoder.encode(
                        JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
