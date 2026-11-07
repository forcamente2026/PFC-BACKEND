package forcamente.api.controller;

import forcamente.api.dto.Mascaras;
import forcamente.api.entity.RegistroAuditoriaEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IRegistroAuditoriaRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private IRegistroAuditoriaRepository registroAuditoriaRepository;

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

    @Test
    @DisplayName("o administrador pede os dados completos e a trilha grava quem olhou qual conta")
    void deveDevolverOsDadosCompletosERegistrarNaTrilha() throws Exception {
        var professor = umProfessor();

        long antes = registroAuditoriaRepository.count();

        mockMvc.perform(get("/api/usuarios/" + professor.getId() + "/dados-completos")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(professor.getEmail()))
                .andExpect(jsonPath("$.cref").value(professor.getCref()))
                .andExpect(jsonPath("$.cep").value(professor.getCep()))
                .andExpect(jsonPath("$.logradouro").value(professor.getLogradouro()));

        assertThat(registroAuditoriaRepository.count())
                .as("olhar dado pessoal de outra conta tem de deixar rastro")
                .isEqualTo(antes + 1);

        assertThat(registroAuditoriaRepository.findAll().stream()
                .anyMatch(registro -> professor.getId().equals(registro.getRecursoId())
                        && registro.getAcao() == AcaoAuditoriaEnum.DADO_COMPLETO_LIDO))
                .as("a trilha diz qual conta foi aberta, com acao propria")
                .isTrue();
    }

    @Test
    @DisplayName("a trilha distingue a leitura mascarada da abertura dos dados completos")
    void deveDistinguirLeituraMascaradaDeAberturaCompleta() throws Exception {
        var professor = umProfessor();
        var idAdministrador = UUID.randomUUID();
        String token = tokenPara(idAdministrador, "ADMINISTRADOR");

        mockMvc.perform(get("/api/usuarios/" + professor.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios/" + professor.getId() + "/dados-completos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        var daConta = registroAuditoriaRepository.findAll().stream()
                .filter(registro -> professor.getId().equals(registro.getRecursoId())
                        && idAdministrador.equals(registro.getUsuarioId()))
                .toList();

        assertThat(daConta)
                .as("a leitura mascarada e a abertura precisam deixar registros diferentes")
                .extracting(registro -> registro.getAcao())
                .contains(AcaoAuditoriaEnum.LIDO, AcaoAuditoriaEnum.DADO_COMPLETO_LIDO);
    }

    @Test
    @DisplayName("professor e aluno recebem 403 nos dados completos, e sem token 401")
    void deveBarrarQuemNaoEAdministradorNosDadosCompletos() throws Exception {
        var professor = umProfessor();
        String rota = "/api/usuarios/" + professor.getId() + "/dados-completos";

        mockMvc.perform(get(rota)
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(rota)
                        .header("Authorization", "Bearer " + tokenPara(UUID.randomUUID(), "ALUNO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(rota)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a trilha e o CSV saem com o e-mail mascarado, pelo L11")
    void deveMascararOEmailNaTrilhaENoCsv() throws Exception {
        var professor = umProfessor();

        var registro = new RegistroAuditoriaEntity();
        registro.setAcao(AcaoAuditoriaEnum.CRIADO);
        registro.setRecursoTipo(RecursoAuditoriaEnum.USUARIO);
        registro.setRecursoId(professor.getId());
        registro.setUsuarioId(professor.getId());
        registro.setOcorridoEm(LocalDateTime.now());
        registroAuditoriaRepository.saveAndFlush(registro);

        var trilha = mockMvc.perform(get("/api/auditoria")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(trilha)
                .as("a tela da auditoria nao pode trazer e-mail inteiro")
                .contains(Mascaras.email(professor.getEmail()))
                .doesNotContain(professor.getEmail());

        var csv = mockMvc.perform(get("/api/auditoria/csv")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(csv)
                .as("arquivo baixado sai do alcance do expurgo, entao e onde o dado duraria mais")
                .contains(Mascaras.email(professor.getEmail()))
                .doesNotContain(professor.getEmail());
    }

    @Test
    @DisplayName("salvar a conta mascarada responde 400 e nao destroi o e-mail real")
    void deveRecusarOEmailMascaradoNoPut() throws Exception {
        var aluno = usuarioRepository.findByEmail("ana.ramos@exemplo.test").orElseThrow();
        String emailOriginal = aluno.getEmail();

        var mascarado = mockMvc.perform(get("/api/usuarios/" + aluno.getId())
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(mascarado).as("a tela do administrador traz o e-mail mascarado").contains("***");

        mockMvc.perform(put("/api/usuarios/" + aluno.getId())
                        .header("Authorization", "Bearer " + tokenDeAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeCompleto": "Ana Ramos Corrigida",
                                  "email": "%s"
                                }
                                """.formatted(Mascaras.email(emailOriginal))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(org.hamcrest.Matchers.hasItem("email")));

        assertThat(usuarioRepository.findById(aluno.getId()).orElseThrow().getEmail())
                .as("o e-mail real nao pode ser substituido pela mascara: a pessoa perderia o acesso")
                .isEqualTo(emailOriginal);
    }

    @Test
    @DisplayName("a fila de pedidos de exclusao sai com o e-mail mascarado")
    void deveMascararAFilaDePedidosDeExclusao() throws Exception {
        var aluno = usuarioRepository.findByEmail("bruno.lima@exemplo.test").orElseThrow();
        aluno.setAnonimizacaoSolicitadaEm(LocalDateTime.now());
        usuarioRepository.saveAndFlush(aluno);

        var json = mockMvc.perform(get("/api/usuarios/anonimizacoes-pendentes")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(json)
                .as("a fila de exclusao e tela de LGPD, e era o quarto DTO com e-mail inteiro")
                .contains(Mascaras.email(aluno.getEmail()))
                .doesNotContain(aluno.getEmail());
    }

    @Test
    @DisplayName("a busca da gestao acha o e-mail por igualdade exata e nao por pedaco")
    void deveBuscarOEmailSoPorIgualdadeExata() throws Exception {
        var aluno = usuarioRepository.findByEmail("ana.ramos@exemplo.test").orElseThrow();

        mockMvc.perform(get("/api/usuarios")
                        .param("busca", aluno.getEmail())
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeItens").value(1));

        mockMvc.perform(get("/api/usuarios")
                        .param("busca", "ana.ram")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[?(@.id == '" + aluno.getId() + "')]").isEmpty());
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
