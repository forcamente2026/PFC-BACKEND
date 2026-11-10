package forcamente.api.repository;

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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Quiz no banco - a V8, o mapeamento e as consultas")
class QuizRepositoryTest {

    private static final Set<StatusAprovacaoEnum> PUBLICADOS = Set.of(
            StatusAprovacaoEnum.APROVADO,
            StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);

    @Autowired
    private IQuizRepository quizRepository;

    @Autowired
    private IQuizAprovacaoRepository aprovacaoRepository;

    @Autowired
    private IQuizTentativaRepository tentativaRepository;

    @Autowired
    private IArtigoRepository artigoRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("salva o quiz inteiro numa operacao: a cascata grava questoes e alternativas sem violar o NOT NULL da chave")
    void deveSalvarOGrafoInteiro() {
        var autor = professores().getFirst();

        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, autor, List.of());

        entityManager.flush();
        entityManager.clear();

        var lido = quizRepository.findById(quiz.getId()).orElseThrow();

        assertThat(lido.getQuestoes())
                .as("o @OneToMany com @JoinColumn(nullable = false) grava o quiz_id junto do insert")
                .hasSize(2);
        assertThat(lido.getQuestoes().getFirst().getAlternativas()).hasSize(3);
    }

    @Test
    @DisplayName("a ordem das questoes e das alternativas e a que foi gravada")
    void devePreservarAOrdem() {
        var autor = professores().getFirst();
        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, autor, List.of());

        entityManager.flush();
        entityManager.clear();

        var lido = quizRepository.findById(quiz.getId()).orElseThrow();

        assertThat(lido.getQuestoes())
                .extracting(QuizQuestaoEntity::getEnunciado)
                .containsExactly("Primeira pergunta", "Segunda pergunta");

        assertThat(lido.getQuestoes().getFirst().getAlternativas())
                .extracting(QuizAlternativaEntity::getTexto)
                .containsExactly("Alternativa A", "Alternativa B", "Alternativa C");
    }

    @Test
    @DisplayName("a leitura sugerida guarda os ids dos artigos na ordem, pela colecao de elementos")
    void deveGuardarOsArtigosSugeridos() {
        var autor = professores().getFirst();
        var primeiro = artigoSalvo(autor);
        var segundo = artigoSalvo(autor);

        var quiz = quizSalvo(StatusAprovacaoEnum.APROVADO, autor,
                List.of(primeiro.getId(), segundo.getId()));

        entityManager.flush();
        entityManager.clear();

        var lido = quizRepository.findById(quiz.getId()).orElseThrow();

        assertThat(lido.getArtigoIds()).containsExactly(primeiro.getId(), segundo.getId());
    }

    @Test
    @DisplayName("a JPQL da visibilidade executa e mostra o rejeitado so para o proprio autor")
    void deveExecutarAJpqlDaVisibilidade() {
        var professores = professores();
        var autor = professores.get(0);
        var outroAutor = professores.get(1);

        var meuRejeitado = quizSalvo(StatusAprovacaoEnum.REJEITADO, autor, List.of());
        var rejeitadoDeOutro = quizSalvo(StatusAprovacaoEnum.REJEITADO, outroAutor, List.of());

        var visiveis = quizRepository.buscarVisiveis(PUBLICADOS, StatusAprovacaoEnum.REJEITADO, autor)
                .stream()
                .map(QuizEntity::getId)
                .toList();

        assertThat(visiveis).contains(meuRejeitado.getId());
        assertThat(visiveis).doesNotContain(rejeitadoDeOutro.getId());
    }

    @Test
    @DisplayName("a consulta por artigo indicado acha o quiz publicado e ignora o pendente")
    void deveAcharOQuizPublicadoQueIndicaOArtigo() {
        var autor = professores().getFirst();
        var artigo = artigoSalvo(autor);

        var publicado = quizSalvo(StatusAprovacaoEnum.APROVADO, autor, List.of(artigo.getId()));
        var pendente = quizSalvo(StatusAprovacaoEnum.PENDENTE, autor, List.of(artigo.getId()));

        var encontrados = quizRepository.buscarPorArtigoIndicado(artigo.getId(), PUBLICADOS)
                .stream()
                .map(QuizEntity::getId)
                .toList();

        assertThat(encontrados).containsExactly(publicado.getId());
        assertThat(encontrados)
                .as("quiz fora do ar nao impede a exclusao do artigo")
                .doesNotContain(pendente.getId());
    }

    @Test
    @DisplayName("apagar o quiz leva questoes, alternativas, artigos sugeridos e aprovacoes")
    void deveApagarODependenteJuntoDoQuiz() {
        var professores = professores();
        var autor = professores.get(0);
        var professorQueAprova = professores.get(1);
        var artigo = artigoSalvo(autor);

        var quiz = quizSalvo(StatusAprovacaoEnum.PENDENTE, autor, List.of(artigo.getId()));

        var aprovacao = new QuizAprovacaoEntity();
        aprovacao.setQuizId(quiz.getId());
        aprovacao.setProfessorId(professorQueAprova);
        aprovacao.setAprovadoEm(LocalDateTime.now());
        aprovacaoRepository.saveAndFlush(aprovacao);

        assertThat(aprovacaoRepository.countByQuizId(quiz.getId())).isEqualTo(1);

        quizRepository.delete(quiz);
        entityManager.flush();

        assertThat(quizRepository.findById(quiz.getId())).isEmpty();
        assertThat(aprovacaoRepository.countByQuizId(quiz.getId()))
                .as("o ON DELETE CASCADE da V8 limpa as aprovacoes")
                .isZero();
    }

    @Test
    @DisplayName("as consultas das tentativas executam")
    void deveExecutarAsConsultasDasTentativas() {
        var autor = professores().getFirst();
        var quiz = quizSalvo(StatusAprovacaoEnum.APROVADO, autor, List.of());

        assertThatCode(() -> tentativaRepository.findByUsuarioIdAndQuizIdIn(autor, Set.of(quiz.getId())))
                .doesNotThrowAnyException();
        assertThatCode(() -> tentativaRepository.deleteByUsuarioId(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    private QuizEntity quizSalvo(StatusAprovacaoEnum status, UUID autorId, List<UUID> artigoIds) {
        var quiz = new QuizEntity();

        quiz.setTitulo("Quiz de teste de repositorio");
        quiz.setDescricao("Descricao de teste do quiz.");
        quiz.setCategoria(CategoriaArtigoEnum.FISIOLOGIA);
        quiz.setNivel(NivelDificuldadeEnum.INICIANTE);
        quiz.setStatus(status);
        quiz.setAutorId(autorId);
        quiz.setCriadoEm(LocalDateTime.now());
        quiz.getQuestoes().addAll(List.of(
                questao("Primeira pergunta"),
                questao("Segunda pergunta")));
        quiz.getArtigoIds().addAll(artigoIds);

        return quizRepository.save(quiz);
    }

    private QuizQuestaoEntity questao(String enunciado) {
        var questao = new QuizQuestaoEntity();

        questao.setEnunciado(enunciado);
        questao.setExplicacao("Explicacao de teste.");
        questao.getAlternativas().addAll(List.of(
                alternativa("Alternativa A", true),
                alternativa("Alternativa B", false),
                alternativa("Alternativa C", false)));

        return questao;
    }

    private QuizAlternativaEntity alternativa(String texto, boolean correta) {
        var alternativa = new QuizAlternativaEntity();

        alternativa.setTexto(texto);
        alternativa.setCorreta(correta);

        return alternativa;
    }

    private ArtigoEntity artigoSalvo(UUID autorId) {
        var artigo = new ArtigoEntity();

        artigo.setTitulo("Artigo indicado em quiz");
        artigo.setCategoria(CategoriaArtigoEnum.FISIOLOGIA);
        artigo.setResumo("Resumo de teste. ".repeat(20));
        artigo.setFonte("https://doi.org/teste/" + UUID.randomUUID());
        artigo.setStatus(StatusAprovacaoEnum.APROVADO);
        artigo.setAutorId(autorId);
        artigo.setCriadoEm(LocalDateTime.now());

        return artigoRepository.save(artigo);
    }

    private List<UUID> professores() {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores)
                .as("a V3 semeia professores e a V7 os deixa aprovados")
                .hasSizeGreaterThanOrEqualTo(2);

        return professores.stream().map(UsuarioEntity::getId).toList();
    }
}
