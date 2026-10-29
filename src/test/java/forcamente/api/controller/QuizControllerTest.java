package forcamente.api.controller;

import forcamente.api.entity.QuizAlternativaEntity;
import forcamente.api.entity.QuizEntity;
import forcamente.api.entity.QuizQuestaoEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IQuizRepository;
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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("QuizController - a leitura, e o gabarito que nao pode vazar")
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IQuizRepository quizRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Test
    @DisplayName("o aluno nao recebe o gabarito: nenhuma alternativa diz se e a correta")
    void deveEsconderOGabaritoDoAluno() throws Exception {
        var quiz = quizSalvo(StatusAprovacaoEnum.APROVADO, professores().getFirst());

        mockMvc.perform(get("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gabarito").value(false))
                .andExpect(jsonPath("$.questoes[0].explicacao").value(nullValue()))
                .andExpect(jsonPath("$.questoes[0].alternativas[*].correta", everyItem(nullValue())))
                .andExpect(jsonPath("$.questoes[1].alternativas[*].correta", everyItem(nullValue())));
    }

    @Test
    @DisplayName("o autor recebe o gabarito, com a explicacao e uma alternativa correta")
    void deveEntregarOGabaritoAoAutor() throws Exception {
        var autor = professores().getFirst();
        var quiz = quizSalvo(StatusAprovacaoEnum.APROVADO, autor);

        mockMvc.perform(get("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gabarito").value(true))
                .andExpect(jsonPath("$.questoes[0].explicacao").value("Explicacao de teste."))
                .andExpect(jsonPath("$.questoes[0].alternativas[0].correta").value(true))
                .andExpect(jsonPath("$.questoes[0].alternativas[1].correta").value(false));
    }

    @Test
    @DisplayName("o detalhe e um JSON plano e traz os nomes do contrato")
    void deveDevolverOsNomesDoContratoNumObjetoPlano() throws Exception {
        var autor = professores().getFirst();
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, autor);

        mockMvc.perform(get("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Principios da hipertrofia"))
                .andExpect(jsonPath("$.descricao").exists())
                .andExpect(jsonPath("$.categoria.codigo").value("FISIOLOGIA"))
                .andExpect(jsonPath("$.categoria.descricao").value("Fisiologia"))
                .andExpect(jsonPath("$.nivel.codigo").value("INICIANTE"))
                .andExpect(jsonPath("$.nivel.descricao").value("Iniciante"))
                .andExpect(jsonPath("$.autorNome").exists())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.motivoRejeicao").value(nullValue()))
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(0))
                .andExpect(jsonPath("$.aprovacoesNecessarias").value(3))
                .andExpect(jsonPath("$.podeEditar").value(true))
                .andExpect(jsonPath("$.podeAprovar").value(false))
                .andExpect(jsonPath("$.professoresQueAprovaram").isEmpty())
                .andExpect(jsonPath("$.totalQuestoes").value(2))
                .andExpect(jsonPath("$.notaMinima").value(70))
                .andExpect(jsonPath("$.melhorTentativa").value(nullValue()))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.artigos").isEmpty())
                .andExpect(jsonPath("$.questoes", hasSize(2)))
                .andExpect(jsonPath("$.questoes[0].enunciado").value("Primeira pergunta"))
                .andExpect(jsonPath("$.questoes[0].alternativas[0].texto").value("Alternativa A"))
                .andExpect(jsonPath("$.resumo").doesNotExist());
    }

    @Test
    @DisplayName("quiz pendente de outra pessoa responde 404 para o aluno, nao 403")
    void deveResponder404ParaQuizQueOAlunoNaoVe() throws Exception {
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores().getFirst());

        mockMvc.perform(get("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("o autor ve o proprio quiz rejeitado, com o motivo")
    void deveMostrarORejeitadoAoSeuAutor() throws Exception {
        var autor = professores().getFirst();
        var quiz = quizSalvo(StatusAprovacaoEnum.REJEITADO, autor);

        mockMvc.perform(get("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Motivo de teste."));
    }

    @Test
    @DisplayName("a listagem esconde do aluno o quiz pendente")
    void deveEsconderOPendenteDaListagemDoAluno() throws Exception {
        var pendente = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores().getFirst());

        mockMvc.perform(get("/api/quizzes")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + pendente.getId() + "')]").isEmpty());
    }

    @Test
    @DisplayName("aluno na fila de pendentes recebe 403")
    void deveRecusarAlunoNaFilaDePendentes() throws Exception {
        mockMvc.perform(get("/api/quizzes/pendentes")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o administrador ve a fila de pendentes")
    void deveListarAFilaParaOAdministrador() throws Exception {
        mockMvc.perform(get("/api/quizzes/pendentes")
                        .header("Authorization", "Bearer " + tokenDe("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("sem token, a listagem responde 401")
    void deveResponder401SemToken() throws Exception {
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isUnauthorized());
    }

    private QuizEntity quizSalvo(StatusAprovacaoEnum status, UUID autorId) {
        var quiz = new QuizEntity();

        quiz.setTitulo("Principios da hipertrofia");
        quiz.setDescricao("Confira o que voce sabe sobre o que faz o musculo crescer.");
        quiz.setCategoria(CategoriaArtigoEnum.FISIOLOGIA);
        quiz.setNivel(NivelDificuldadeEnum.INICIANTE);
        quiz.setStatus(status);
        quiz.setMotivoRejeicao(status == StatusAprovacaoEnum.REJEITADO ? "Motivo de teste." : null);
        quiz.setAutorId(autorId);
        quiz.setCriadoEm(LocalDateTime.now());
        quiz.getQuestoes().addAll(List.of(
                questao("Primeira pergunta"),
                questao("Segunda pergunta")));

        return quizRepository.saveAndFlush(quiz);
    }

    private QuizQuestaoEntity questao(String enunciado) {
        var questao = new QuizQuestaoEntity();

        questao.setEnunciado(enunciado);
        questao.setExplicacao("Explicacao de teste.");
        questao.getAlternativas().addAll(List.of(
                alternativa("Alternativa A", true),
                alternativa("Alternativa B", false)));

        return questao;
    }

    private QuizAlternativaEntity alternativa(String texto, boolean correta) {
        var alternativa = new QuizAlternativaEntity();

        alternativa.setTexto(texto);
        alternativa.setCorreta(correta);

        return alternativa;
    }

    private List<UUID> professores() {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores)
                .as("a V3 semeia professores e a V7 os deixa aprovados")
                .isNotEmpty();

        return professores.stream().map(UsuarioEntity::getId).toList();
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
