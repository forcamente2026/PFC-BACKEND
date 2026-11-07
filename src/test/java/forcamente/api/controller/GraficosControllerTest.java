package forcamente.api.controller;

import forcamente.api.entity.QuizAlternativaEntity;
import forcamente.api.entity.QuizEntity;
import forcamente.api.entity.QuizQuestaoEntity;
import forcamente.api.entity.QuizTentativaEntity;
import forcamente.api.entity.RegistroAuditoriaEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IQuizRepository;
import forcamente.api.repository.IQuizTentativaRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Graficos do Inicio - historico, desempenho e as duas estatisticas")
class GraficosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IQuizRepository quizRepository;

    @Autowired
    private IQuizTentativaRepository quizTentativaRepository;

    @Autowired
    private IRegistroAuditoriaRepository registroAuditoriaRepository;

    @Test
    @DisplayName("minhas-tentativas devolve so as tentativas de quem pede, da mais antiga para a mais nova")
    void deveDevolverSoOHistoricoDeQuemPede() throws Exception {
        var aluno = umAluno();
        var outro = outroAluno();
        var quiz = quizSalvo(umProfessor().getId(), "Principios da hipertrofia");

        tentativaSalva(quiz.getId(), aluno.getId(), 2, 5, LocalDateTime.now().minusDays(2));
        tentativaSalva(quiz.getId(), aluno.getId(), 4, 5, LocalDateTime.now().minusDays(1));
        tentativaSalva(quiz.getId(), outro.getId(), 5, 5, LocalDateTime.now());

        mockMvc.perform(get("/api/quizzes/minhas-tentativas")
                        .header("Authorization", "Bearer " + tokenPara(aluno.getId(), "ALUNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].quiz").value("Principios da hipertrofia"))
                .andExpect(jsonPath("$[0].acertos").value(2))
                .andExpect(jsonPath("$[0].total").value(5))
                .andExpect(jsonPath("$[0].percentual").value(40))
                .andExpect(jsonPath("$[1].percentual").value(80));
    }

    @Test
    @DisplayName("sem tentativa nenhuma, minhas-tentativas responde lista vazia e nao 404")
    void deveDevolverListaVaziaSemTentativas() throws Exception {
        mockMvc.perform(get("/api/quizzes/minhas-tentativas")
                        .header("Authorization", "Bearer " + tokenPara(UUID.randomUUID(), "ALUNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("o caminho literal minhas-tentativas ganha do GET /quizzes/{id}")
    void deveGanharDoCaminhoComId() throws Exception {
        mockMvc.perform(get("/api/quizzes/minhas-tentativas")
                        .header("Authorization", "Bearer " + tokenPara(UUID.randomUUID(), "ALUNO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("desempenho traz so os quizzes de quem pede, sem as tentativas do proprio autor")
    void deveTrazerODesempenhoSemAsTentativasDoAutor() throws Exception {
        var professor = umProfessor();
        var aluno = umAluno();
        var outro = outroAluno();

        var meuQuiz = quizSalvo(professor.getId(), "Quiz do professor que pede");
        var quizDeOutro = quizSalvo(outroProfessor().getId(), "Quiz de outro professor");

        tentativaSalva(meuQuiz.getId(), aluno.getId(), 2, 5, LocalDateTime.now());
        tentativaSalva(meuQuiz.getId(), outro.getId(), 4, 5, LocalDateTime.now());
        tentativaSalva(meuQuiz.getId(), professor.getId(), 5, 5, LocalDateTime.now());
        tentativaSalva(quizDeOutro.getId(), aluno.getId(), 5, 5, LocalDateTime.now());

        mockMvc.perform(get("/api/quizzes/meus/desempenho")
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quiz").value("Quiz do professor que pede"))
                .andExpect(jsonPath("$[0].tentativas").value(2))
                .andExpect(jsonPath("$[0].notaMedia").value(60))
                .andExpect(jsonPath("$[0].aluno").doesNotExist())
                .andExpect(jsonPath("$[0].usuarioId").doesNotExist());
    }

    @Test
    @DisplayName("aluno no desempenho recebe 403, e sem token 401")
    void deveBarrarAlunoNoDesempenho() throws Exception {
        mockMvc.perform(get("/api/quizzes/meus/desempenho")
                        .header("Authorization", "Bearer " + tokenPara(umAluno().getId(), "ALUNO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/quizzes/meus/desempenho"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("contas-por-papel traz os tres papeis e nao conta conta anonimizada")
    void deveContarContasPorPapelSemAsAnonimizadas() throws Exception {
        long alunosAntes = usuarioRepository.countByPapelAndAnonimizadoEmIsNull(PapelUsuarioEnum.ALUNO);

        var anonimizado = outroAluno();
        anonimizado.setAnonimizadoEm(LocalDateTime.now());
        usuarioRepository.saveAndFlush(anonimizado);

        mockMvc.perform(get("/api/estatisticas/contas-por-papel")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].papel").value("ALUNO"))
                .andExpect(jsonPath("$[0].total").value(alunosAntes - 1))
                .andExpect(jsonPath("$[1].papel").value("PROFESSOR"))
                .andExpect(jsonPath("$[2].papel").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("atividade-por-dia devolve os dias com zero e nao conta a acao LIDO")
    void deveDevolverOsDiasComZeroSemContarLeitura() throws Exception {
        registroSalvo(AcaoAuditoriaEnum.CRIADO, LocalDateTime.now());
        registroSalvo(AcaoAuditoriaEnum.LIDO, LocalDateTime.now());
        registroSalvo(AcaoAuditoriaEnum.LIDO, LocalDateTime.now());

        var resposta = mockMvc.perform(get("/api/estatisticas/atividade-por-dia")
                        .param("dias", "3")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        assertThat(resposta.getResponse().getContentAsString())
                .as("o dia de hoje e o ultimo da lista")
                .contains(java.time.LocalDate.now().toString());
    }

    @Test
    @DisplayName("dias fora da faixa de 1 a 90 responde 400")
    void deveRecusarDiasForaDaFaixa() throws Exception {
        mockMvc.perform(get("/api/estatisticas/atividade-por-dia")
                        .param("dias", "0")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/estatisticas/atividade-por-dia")
                        .param("dias", "91")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("aluno e professor recebem 403 nas estatisticas, e sem token 401")
    void deveBarrarQuemNaoEAdministradorNasEstatisticas() throws Exception {
        for (String rota : List.of("/api/estatisticas/contas-por-papel",
                "/api/estatisticas/atividade-por-dia")) {

            mockMvc.perform(get(rota)
                            .header("Authorization", "Bearer " + tokenPara(umAluno().getId(), "ALUNO")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(rota)
                            .header("Authorization", "Bearer " + tokenPara(umProfessor().getId(), "PROFESSOR")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(rota))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("as duas estatisticas nao gravam nada na trilha, pela excecao aceita a RN32")
    void naoDeveGravarNaTrilha() throws Exception {
        long antes = registroAuditoriaRepository.count();

        mockMvc.perform(get("/api/estatisticas/contas-por-papel")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estatisticas/atividade-por-dia")
                        .header("Authorization", "Bearer " + tokenDeAdministrador()))
                .andExpect(status().isOk());

        assertThat(registroAuditoriaRepository.count())
                .as("abrir o Inicio nao pode encher a trilha")
                .isEqualTo(antes);
    }

    private UsuarioEntity umAluno() {
        return porEmail("ana.ramos@exemplo.test");
    }

    private UsuarioEntity outroAluno() {
        return porEmail("bruno.lima@exemplo.test");
    }

    private UsuarioEntity umProfessor() {
        return professores().get(0);
    }

    private UsuarioEntity outroProfessor() {
        return professores().get(1);
    }

    private List<UsuarioEntity> professores() {
        var encontrados = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(encontrados).as("a V3 semeia professores aprovados").hasSizeGreaterThanOrEqualTo(2);

        return encontrados;
    }

    private UsuarioEntity porEmail(String email) {
        var encontrado = usuarioRepository.findByEmail(email);

        assertThat(encontrado).as("a V3 semeia a conta %s", email).isPresent();

        return encontrado.get();
    }

    private QuizEntity quizSalvo(UUID autorId, String titulo) {
        var alternativa = new QuizAlternativaEntity();
        alternativa.setTexto("Alternativa A");
        alternativa.setCorreta(true);

        var outra = new QuizAlternativaEntity();
        outra.setTexto("Alternativa B");
        outra.setCorreta(false);

        var questao = new QuizQuestaoEntity();
        questao.setEnunciado("Pergunta do quiz de teste");
        questao.getAlternativas().addAll(List.of(alternativa, outra));

        var quiz = new QuizEntity();
        quiz.setTitulo(titulo);
        quiz.setDescricao("Descricao de teste.");
        quiz.setCategoria(CategoriaArtigoEnum.FISIOLOGIA);
        quiz.setNivel(NivelDificuldadeEnum.INICIANTE);
        quiz.setStatus(StatusAprovacaoEnum.APROVADO);
        quiz.setAutorId(autorId);
        quiz.setCriadoEm(LocalDateTime.now());
        quiz.getQuestoes().add(questao);

        return quizRepository.saveAndFlush(quiz);
    }

    private void tentativaSalva(UUID quizId, UUID usuarioId, int acertos, int total, LocalDateTime feitaEm) {
        var tentativa = new QuizTentativaEntity();

        tentativa.setQuizId(quizId);
        tentativa.setUsuarioId(usuarioId);
        tentativa.setAcertos(acertos);
        tentativa.setTotal(total);
        tentativa.setFeitaEm(feitaEm);

        quizTentativaRepository.saveAndFlush(tentativa);
    }

    private void registroSalvo(AcaoAuditoriaEnum acao, LocalDateTime ocorridoEm) {
        var registro = new RegistroAuditoriaEntity();

        registro.setAcao(acao);
        registro.setRecursoTipo(RecursoAuditoriaEnum.USUARIO);
        registro.setRecursoId(UUID.randomUUID());
        registro.setUsuarioId(umAluno().getId());
        registro.setOcorridoEm(ocorridoEm);

        registroAuditoriaRepository.saveAndFlush(registro);
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
