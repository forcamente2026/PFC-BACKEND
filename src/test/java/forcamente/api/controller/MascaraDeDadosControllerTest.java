package forcamente.api.controller;

import forcamente.api.dto.Mascaras;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Mascara de dados pessoais - o titular ve inteiro, todos os outros veem mascarado")
class MascaraDeDadosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Test
    @DisplayName("o titular ve os proprios dados sem mascara, pelo art. 18 III")
    void deveDevolverOsProprosDadosSemMascara() throws Exception {
        var professor = umProfessor();

        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(professor.getEmail()))
                .andExpect(jsonPath("$.cref").value(professor.getCref()))
                .andExpect(jsonPath("$.cep").value(professor.getCep()))
                .andExpect(jsonPath("$.logradouro").value(professor.getLogradouro()))
                .andExpect(jsonPath("$.dataNascimento").value(matchesPattern("\\d{2}/\\d{2}/\\d{4}")));
    }

    @Test
    @DisplayName("o administrador ve a lista mascarada: e-mail, nascimento, CREF e endereco reduzido")
    void deveMascararAListaParaOAdministrador() throws Exception {
        var professor = umProfessor();

        mockMvc.perform(get("/api/usuarios")
                        .param("busca", professor.getEmail())
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].email")
                        .value(startsWith(professor.getEmail().substring(0, 1) + "***@")))
                .andExpect(jsonPath("$.conteudo[0].dataNascimento")
                        .value(matchesPattern("\\*\\*/\\*\\*/\\d{4}")))
                .andExpect(jsonPath("$.conteudo[0].cref").value(matchesPattern("\\d\\*\\*\\*\\d{2}-[A-Z]/[A-Z]{2}")))
                .andExpect(jsonPath("$.conteudo[0].cep").value(nullValue()))
                .andExpect(jsonPath("$.conteudo[0].logradouro").value(nullValue()))
                .andExpect(jsonPath("$.conteudo[0].numero").value(nullValue()))
                .andExpect(jsonPath("$.conteudo[0].bairro").value(nullValue()))
                .andExpect(jsonPath("$.conteudo[0].cidade").value(professor.getCidade()))
                .andExpect(jsonPath("$.conteudo[0].estado").value(professor.getEstado()));
    }

    @Test
    @DisplayName("a fila de verificacao do CREF sai mascarada, mesmo sendo a tela de conferir o CREF")
    void deveMascararAFilaDeVerificacao() throws Exception {
        var pendentes = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        var professor = pendentes.getFirst();
        professor.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);
        usuarioRepository.saveAndFlush(professor);

        var json = mockMvc.perform(get("/api/usuarios/pendentes-verificacao")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(json)
                .as("a fila traz a conta que acabou de ficar pendente")
                .contains(professor.getId().toString())
                .contains(Mascaras.email(professor.getEmail()))
                .contains(Mascaras.cref(professor.getCref()));

        assertThat(json)
                .as("o valor real nao pode aparecer em lugar nenhum da resposta")
                .doesNotContain(professor.getEmail())
                .doesNotContain(professor.getCref());
    }

    @Test
    @DisplayName("o administrador buscando outra conta por id recebe mascarado")
    void deveMascararAContaDeOutroParaOAdministrador() throws Exception {
        var professor = umProfessor();

        mockMvc.perform(get("/api/usuarios/" + professor.getId())
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(startsWith(
                        professor.getEmail().substring(0, 1) + "***@")))
                .andExpect(jsonPath("$.cref").value(matchesPattern("\\d\\*\\*\\*\\d{2}-[A-Z]/[A-Z]{2}")));
    }

    @Test
    @DisplayName("o titular buscando a propria conta por id recebe sem mascara")
    void deveDevolverAPropriaContaPorIdSemMascara() throws Exception {
        var professor = umProfessor();

        mockMvc.perform(get("/api/usuarios/" + professor.getId())
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(professor.getEmail()))
                .andExpect(jsonPath("$.cref").value(professor.getCref()));
    }

    private UsuarioEntity umProfessor() {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores).as("a V3 semeia professor com CREF, endereco e nascimento").isNotEmpty();

        var professor = professores.getFirst();

        assertThat(professor.getCref()).as("o professor semeado precisa ter CREF").isNotBlank();
        assertThat(professor.getCep()).as("o professor semeado precisa ter endereco").isNotBlank();

        return professor;
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
