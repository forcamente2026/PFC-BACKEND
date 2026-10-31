package forcamente.api.controller;

import forcamente.api.entity.ArtigoEntity;
import forcamente.api.entity.QuizAlternativaEntity;
import forcamente.api.entity.QuizAprovacaoEntity;
import forcamente.api.entity.QuizEntity;
import forcamente.api.entity.QuizQuestaoEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IArtigoRepository;
import forcamente.api.repository.IQuizAprovacaoRepository;
import forcamente.api.repository.IQuizRepository;
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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("QuizController - leitura, escrita, e o gabarito que nao pode vazar")
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IQuizRepository quizRepository;

    @Autowired
    private IQuizAprovacaoRepository quizAprovacaoRepository;

    @Autowired
    private IArtigoRepository artigoRepository;

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

    @Test
    @DisplayName("o professor cria e o quiz nasce pendente, com o gabarito visivel para ele")
    void deveCriarComoProfessorENascerPendente() throws Exception {
        var autor = professores().getFirst();

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("Primeira pergunta", true, false))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.totalQuestoes").value(3))
                .andExpect(jsonPath("$.podeEditar").value(true))
                .andExpect(jsonPath("$.gabarito").value(true))
                .andExpect(jsonPath("$.questoes[0].alternativas[0].correta").value(true));
    }

    @Test
    @DisplayName("o quiz do administrador nasce aprovado")
    void deveNascerAprovadoQuandoOAutorEAdministrador() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + tokenDe("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("Primeira pergunta", true, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    @DisplayName("aluno criando quiz recebe 403")
    void deveRecusarAlunoCriandoQuiz() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("Primeira pergunta", true, false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o erro de alternativa aponta a pergunta e a alternativa pelo indice")
    void deveApontarOCampoComIndice() throws Exception {
        var autor = professores().getFirst();

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("", "", true, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos"))
                .andExpect(jsonPath("$.campos[*].campo",
                        hasItem("questoes[0].alternativas[0].texto")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("questoes[0].enunciado")));
    }

    @Test
    @DisplayName("duas alternativas corretas apontam o campo umaAlternativaCorreta da pergunta")
    void deveRecusarDuasAlternativasCorretas() throws Exception {
        var autor = professores().getFirst();

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("Primeira pergunta", true, true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo",
                        hasItem("questoes[0].umaAlternativaCorreta")));
    }

    @Test
    @DisplayName("marcacao no enunciado responde 400 e aponta o campo, mas comparacao numerica passa")
    void deveRecusarMarcacaoEAceitarComparacao() throws Exception {
        var autor = professores().getFirst();
        var token = tokenPara(autor, "PROFESSOR");

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("<script>alert(1)</script>", true, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("questoes[0].enunciado")));

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("Qual carga e > 80% de 1RM?", true, false))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("artigo indicado que nao esta publicado responde 400 sem lista de campos")
    void deveRecusarArtigoNaoPublicado() throws Exception {
        var autor = professores().getFirst();
        var pendente = artigoSalvo(autor, StatusAprovacaoEnum.PENDENTE);

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[\"" + pendente.getId() + "\"]",
                                questaoJson("Primeira pergunta", true, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isEmpty());
    }

    @Test
    @DisplayName("a edicao do autor devolve o quiz para pendente e zera as aprovacoes")
    void deveVoltarParaPendenteNaEdicao() throws Exception {
        var professores = professores();
        var autor = professores.get(0);
        var quiz = quizSalvo(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES, autor);

        var aprovacao = new QuizAprovacaoEntity();
        aprovacao.setQuizId(quiz.getId());
        aprovacao.setProfessorId(professores.get(1));
        aprovacao.setAprovadoEm(LocalDateTime.now());
        quizAprovacaoRepository.saveAndFlush(aprovacao);

        mockMvc.perform(put("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("Pergunta trocada", true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(0))
                .andExpect(jsonPath("$.questoes[0].enunciado").value("Pergunta trocada"));
    }

    @Test
    @DisplayName("professor editando quiz de outra pessoa recebe 403")
    void deveRecusarEdicaoDeOutroProfessor() throws Exception {
        var professores = professores();
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores.get(0));

        mockMvc.perform(put("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenPara(professores.get(1), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("[]", questaoJson("Primeira pergunta", true, false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a rota de edicao devolve o corpo de criacao, com o gabarito, so para o autor")
    void deveDevolverOCorpoDeCriacaoNaEdicao() throws Exception {
        var professores = professores();
        var autor = professores.get(0);
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, autor);

        mockMvc.perform(get("/api/quizzes/" + quiz.getId() + "/edicao")
                        .header("Authorization", "Bearer " + tokenPara(autor, "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("FISIOLOGIA"))
                .andExpect(jsonPath("$.nivel").value("INICIANTE"))
                .andExpect(jsonPath("$.artigos").isEmpty())
                .andExpect(jsonPath("$.questoes[0].alternativas[0].correta").value(true));

        mockMvc.perform(get("/api/quizzes/" + quiz.getId() + "/edicao")
                        .header("Authorization", "Bearer " + tokenPara(professores.get(1), "PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o autor exclui o proprio quiz, e outro professor recebe 403")
    void deveExcluirSomenteComoAutorOuAdministrador() throws Exception {
        var professores = professores();
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores.get(0));

        mockMvc.perform(delete("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenPara(professores.get(1), "PROFESSOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/quizzes/" + quiz.getId())
                        .header("Authorization", "Bearer " + tokenPara(professores.get(0), "PROFESSOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("o professor aprova o quiz de outra pessoa e a contagem sobe")
    void deveAprovarComoProfessor() throws Exception {
        var professores = professores();
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores.get(0));

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/aprovacoes")
                        .header("Authorization", "Bearer " + tokenPara(professores.get(1), "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(1))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("o autor aprovando o proprio quiz recebe 403, e aprovar duas vezes responde 409")
    void deveBarrarAutoaprovacaoEAprovacaoRepetida() throws Exception {
        var professores = professores();
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores.get(0));

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/aprovacoes")
                        .header("Authorization", "Bearer " + tokenPara(professores.get(0), "PROFESSOR")))
                .andExpect(status().isForbidden());

        var outro = tokenPara(professores.get(1), "PROFESSOR");

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/aprovacoes")
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/aprovacoes")
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("tres aprovacoes levam o quiz a aprovado pelos professores")
    void deveAtingirOLimiarDeTresAprovacoes() throws Exception {
        var professores = professores();

        assertThat(professores)
                .as("o limiar exige o autor mais tres aprovadores distintos")
                .hasSizeGreaterThanOrEqualTo(4);

        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores.get(0));

        for (int indice = 1; indice <= 2; indice++) {
            mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/aprovacoes")
                            .header("Authorization", "Bearer " + tokenPara(professores.get(indice), "PROFESSOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDENTE"));
        }

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/aprovacoes")
                        .header("Authorization", "Bearer " + tokenPara(professores.get(3), "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovacoesDeProfessores").value(3))
                .andExpect(jsonPath("$.status").value("APROVADO_PELOS_PROFESSORES"));
    }

    @Test
    @DisplayName("aluno aprovando quiz recebe 403, pela cadeia de seguranca")
    void deveRecusarAlunoAprovando() throws Exception {
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores().getFirst());

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/aprovacoes")
                        .header("Authorization", "Bearer " + tokenDe("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o administrador decide, e decidir de novo responde 409")
    void deveDecidirUmaVezSo() throws Exception {
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores().getFirst());
        var administrador = tokenDe("ADMINISTRADOR");

        mockMvc.perform(patch("/api/quizzes/" + quiz.getId() + "/status")
                        .header("Authorization", "Bearer " + administrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APROVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.motivoRejeicao").value(nullValue()));

        mockMvc.perform(patch("/api/quizzes/" + quiz.getId() + "/status")
                        .header("Authorization", "Bearer " + administrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APROVADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rejeitar sem motivo responde 400, e com motivo grava o motivo")
    void deveExigirMotivoNaRejeicao() throws Exception {
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores().getFirst());
        var administrador = tokenDe("ADMINISTRADOR");

        mockMvc.perform(patch("/api/quizzes/" + quiz.getId() + "/status")
                        .header("Authorization", "Bearer " + administrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJEITADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());

        mockMvc.perform(patch("/api/quizzes/" + quiz.getId() + "/status")
                        .header("Authorization", "Bearer " + administrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJEITADO\",\"motivo\":\"Pergunta 2 esta ambigua.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Pergunta 2 esta ambigua."));
    }

    @Test
    @DisplayName("professor decidindo a situacao recebe 403: a decisao e do administrador")
    void deveRecusarProfessorDecidindo() throws Exception {
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, professores().getFirst());

        mockMvc.perform(patch("/api/quizzes/" + quiz.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenDe("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APROVADO\"}"))
                .andExpect(status().isForbidden());
    }

    private String corpo(String artigosJson, String primeiraQuestao) {
        return """
                {
                  "titulo": "Principios da hipertrofia",
                  "descricao": "Confira o que voce sabe.",
                  "categoria": "FISIOLOGIA",
                  "nivel": "INICIANTE",
                  "artigos": %s,
                  "questoes": [%s, %s, %s]
                }
                """.formatted(artigosJson, primeiraQuestao,
                questaoJson("Segunda pergunta", true, false),
                questaoJson("Terceira pergunta", true, false));
    }

    private String questaoJson(String enunciado, boolean corretaA, boolean corretaB) {
        return questaoJson(enunciado, "Alternativa A", corretaA, corretaB);
    }

    private String questaoJson(String enunciado, String textoA, boolean corretaA, boolean corretaB) {
        return """
                {
                  "enunciado": "%s",
                  "explicacao": "Explicacao de teste.",
                  "alternativas": [
                    { "texto": "%s", "correta": %s },
                    { "texto": "Alternativa B", "correta": %s }
                  ]
                }
                """.formatted(enunciado, textoA, corretaA, corretaB);
    }

    private ArtigoEntity artigoSalvo(UUID autorId, StatusAprovacaoEnum status) {
        var artigo = new ArtigoEntity();

        artigo.setTitulo("Artigo para indicar no quiz");
        artigo.setCategoria(CategoriaArtigoEnum.FISIOLOGIA);
        artigo.setResumo("Resumo de teste. ".repeat(20));
        artigo.setFonte("https://doi.org/teste/" + UUID.randomUUID());
        artigo.setStatus(status);
        artigo.setAutorId(autorId);
        artigo.setCriadoEm(LocalDateTime.now());

        return artigoRepository.saveAndFlush(artigo);
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
