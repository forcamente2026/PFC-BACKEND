package forcamente.api.controller;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IExercicioAprovacaoRepository;
import forcamente.api.repository.IExercicioRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Moderacao de exercicios no banco - o limiar de tres aprovacoes de verdade")
class ExercicioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IExercicioRepository exercicioRepository;

    @Autowired
    private IExercicioAprovacaoRepository exercicioAprovacaoRepository;

    @Test
    @DisplayName("tres aprovacoes de professores diferentes publicam o exercicio")
    void deveMudarParaAprovadoPelosProfessoresNaTerceiraAprovacao() throws Exception {
        var professores = professores();
        var autor = professores.get(0);

        UUID exercicioId = criarComoProfessor(autor);

        assertThat(exercicioRepository.findById(exercicioId).orElseThrow().getStatus())
                .as("exercicio de professor nasce na fila")
                .isEqualTo(StatusAprovacaoEnum.PENDENTE);

        aprovar(exercicioId, professores.get(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(1))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        aprovar(exercicioId, professores.get(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(2))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        aprovar(exercicioId, professores.get(3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(3))
                .andExpect(jsonPath("$.status").value("APROVADO_PELOS_PROFESSORES"));

        assertThat(exercicioRepository.findById(exercicioId).orElseThrow().getStatus())
                .as("a contagem precisa ver a aprovacao gravada na mesma transacao")
                .isEqualTo(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
    }

    @Test
    @DisplayName("o mesmo professor aprovando duas vezes responde 409 e nao conta duas")
    void deveRecusarAAprovacaoRepetidaNoBanco() throws Exception {
        var professores = professores();
        UUID exercicioId = criarComoProfessor(professores.get(0));

        aprovar(exercicioId, professores.get(1)).andExpect(status().isOk());
        aprovar(exercicioId, professores.get(1)).andExpect(status().isConflict());

        assertThat(exercicioAprovacaoRepository.countByExercicioId(exercicioId)).isEqualTo(1);
    }

    @Test
    @DisplayName("a edicao do autor apaga as aprovacoes do banco e devolve o item para a fila")
    void deveApagarAsAprovacoesNaEdicaoDoAutor() throws Exception {
        var professores = professores();
        var autor = professores.get(0);

        UUID exercicioId = criarComoProfessor(autor);

        aprovar(exercicioId, professores.get(1)).andExpect(status().isOk());
        aprovar(exercicioId, professores.get(2)).andExpect(status().isOk());

        assertThat(exercicioAprovacaoRepository.countByExercicioId(exercicioId)).isEqualTo(2);

        var exercicio = exercicioRepository.findById(exercicioId).orElseThrow();

        mockMvc.perform(put("/api/exercicios/" + exercicioId)
                        .header("Authorization", "Bearer " + tokenPara(autor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDoExercicio(exercicio.getNome(), "Descricao corrigida pelo autor.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(0));

        assertThat(exercicioAprovacaoRepository.countByExercicioId(exercicioId))
                .as("votos dados a outro texto nao valem para o texto novo")
                .isZero();
    }

    @Test
    @DisplayName("a fila do administrador traz o exercicio pendente, com o nome do autor")
    void deveListarOExercicioPendenteNaFila() throws Exception {
        var autor = professores().get(0);
        UUID exercicioId = criarComoProfessor(autor);

        mockMvc.perform(get("/api/exercicios/pendentes")
                        .header("Authorization", "Bearer " + tokenPara(UUID.randomUUID(), "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + exercicioId + "')].autorNome")
                        .value(autor.getNomeCompleto()));
    }

    @Test
    @DisplayName("o aluno nao ve o exercicio pendente na biblioteca, e recebe 404 no detalhe")
    void deveEsconderOPendenteDoAluno() throws Exception {
        UUID exercicioId = criarComoProfessor(professores().get(0));

        var aluno = usuarioRepository.findByEmail("ana.ramos@exemplo.test").orElseThrow();
        String token = tokenPara(aluno.getId(), "ALUNO");

        mockMvc.perform(get("/api/exercicios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + exercicioId + "')]").isEmpty());

        mockMvc.perform(get("/api/exercicios/" + exercicioId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("aceita o id do video do YouTube e devolve o campo no corpo")
    void deveGuardarOIdDoVideo() throws Exception {
        var autor = professores().get(0);

        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenPara(autor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComVideo("Exercicio com video " + UUID.randomUUID(), "aBcD1234_-x")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoYoutubeId").value("aBcD1234_-x"));
    }

    @Test
    @DisplayName("recusa o endereco completo do YouTube, porque o campo guarda so o id")
    void deveRecusarUrlNoLugarDoId() throws Exception {
        var autor = professores().get(0);

        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenPara(autor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComVideo("Exercicio com url " + UUID.randomUUID(),
                                "https://www.youtube.com/watch?v=aBcD1234_-x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("videoYoutubeId"));
    }

    @Test
    @DisplayName("campo de video vazio e tratado como ausente, e nao como invalido")
    void deveTratarVideoVazioComoAusente() throws Exception {
        var autor = professores().get(0);

        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenPara(autor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComVideo("Exercicio sem video " + UUID.randomUUID(), "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoYoutubeId").doesNotExist());
    }

    private UUID criarComoProfessor(UsuarioEntity autor) throws Exception {
        String nome = "Exercicio de teste " + UUID.randomUUID();

        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenPara(autor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDoExercicio(nome, "Descricao da execucao do exercicio de teste.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorNome").value(autor.getNomeCompleto()));

        var criado = exercicioRepository.findAll().stream()
                .filter(exercicio -> nome.equals(exercicio.getNome()))
                .findFirst();

        assertThat(criado).as("o exercicio criado precisa estar no banco").isPresent();

        return criado.orElseThrow().getId();
    }

    private ResultActions aprovar(UUID exercicioId, UsuarioEntity professor) throws Exception {

        return mockMvc.perform(post("/api/exercicios/" + exercicioId + "/aprovacoes")
                .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR")));
    }

    private String corpoDoExercicio(String nome, String descricao) {
        return """
                {
                  "nome": "%s",
                  "grupoMuscular": "PEITO",
                  "nivel": "INICIANTE",
                  "descricaoExecucao": "%s"
                }
                """.formatted(nome, descricao);
    }

    private String corpoComVideo(String nome, String videoYoutubeId) {
        return """
                {
                  "nome": "%s",
                  "grupoMuscular": "PEITO",
                  "nivel": "INICIANTE",
                  "descricaoExecucao": "Descricao da execucao do exercicio de teste.",
                  "videoYoutubeId": "%s"
                }
                """.formatted(nome, videoYoutubeId);
    }

    private List<UsuarioEntity> professores() {
        var encontrados = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(encontrados)
                .as("a V3 semeia professores aprovados, e o limiar precisa de autor mais tres")
                .hasSizeGreaterThanOrEqualTo(4);

        return encontrados;
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
