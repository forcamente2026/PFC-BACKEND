package forcamente.api.service.impl;

import forcamente.api.dto.AlternativaResponseDTO;
import forcamente.api.dto.ArtigoSugeridoDTO;
import forcamente.api.dto.NotaDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.QuestaoResponseDTO;
import forcamente.api.dto.QuizDetalheResponseDTO;
import forcamente.api.dto.QuizResumoResponseDTO;
import forcamente.api.entity.ArtigoEntity;
import forcamente.api.entity.QuizAprovacaoEntity;
import forcamente.api.entity.QuizEntity;
import forcamente.api.entity.QuizQuestaoEntity;
import forcamente.api.entity.QuizTentativaEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.repository.IArtigoRepository;
import forcamente.api.repository.IQuizAprovacaoRepository;
import forcamente.api.repository.IQuizRepository;
import forcamente.api.repository.IQuizTentativaRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService implements IQuizService {

    private static final int APROVACOES_DE_PROFESSORES_PARA_PUBLICAR = 3;

    private static final int NOTA_MINIMA_PARA_CONCLUIR = 70;

    private static final Set<StatusAprovacaoEnum> PUBLICADOS = Set.of(
            StatusAprovacaoEnum.APROVADO,
            StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);

    private final IQuizRepository quizRepository;

    private final IQuizAprovacaoRepository quizAprovacaoRepository;

    private final IQuizTentativaRepository quizTentativaRepository;

    private final IArtigoRepository artigoRepository;

    private final IUsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QuizResumoResponseDTO> listarQuizzes() {
        log.info("listarQuizzes");

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quizzes = quizRepository.buscarVisiveis(
                statusVisiveis(papel), StatusAprovacaoEnum.REJEITADO, usuarioId);

        return paraResumo(quizzes, papel, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizResumoResponseDTO> listarPendentes() {
        log.info("listarPendentes");

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quizzes = quizRepository.findByStatusInOrderByCriadoEmAsc(
                Set.of(StatusAprovacaoEnum.PENDENTE));

        return paraResumo(quizzes, papel, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetalheResponseDTO buscarPorId(UUID quizId) {
        log.info("buscarPorId: {}", quizId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz nao encontrado: " + quizId));

        exigirVisibilidade(quiz, papel, usuarioId);

        var resumo = paraResumo(List.of(quiz), papel, usuarioId).getFirst();
        boolean gabarito = gabarito(quiz, papel, usuarioId);

        return QuizDetalheResponseDTO.de(
                resumo, gabarito, artigosSugeridos(quiz), questoes(quiz, gabarito));
    }

    private Set<StatusAprovacaoEnum> statusVisiveis(PapelUsuarioEnum papel) {
        if (papel == PapelUsuarioEnum.ADMINISTRADOR) {
            return Set.of(StatusAprovacaoEnum.values());
        }

        if (papel == PapelUsuarioEnum.PROFESSOR) {
            return Set.of(StatusAprovacaoEnum.APROVADO,
                    StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES,
                    StatusAprovacaoEnum.PENDENTE);
        }

        return Set.of(StatusAprovacaoEnum.APROVADO,
                StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
    }

    private void exigirVisibilidade(QuizEntity quiz, PapelUsuarioEnum papel, UUID usuarioId) {
        if (statusVisiveis(papel).contains(quiz.getStatus())) {
            return;
        }

        if (quiz.getStatus() == StatusAprovacaoEnum.REJEITADO
                && quiz.getAutorId().equals(usuarioId)) {

            return;
        }

        throw new RecursoNaoEncontradoException("Quiz nao encontrado: " + quiz.getId());
    }

    private List<QuizResumoResponseDTO> paraResumo(List<QuizEntity> quizzes,
                                                   PapelUsuarioEnum papel,
                                                   UUID usuarioId) {
        if (quizzes.isEmpty()) {
            return List.of();
        }

        var ids = quizzes.stream().map(QuizEntity::getId).toList();

        var aprovacoes = quizAprovacaoRepository.findByQuizIdIn(ids);
        var nomes = buscarNomes(quizzes, aprovacoes);
        var melhores = melhoresTentativas(ids, usuarioId);

        return quizzes.stream()
                .map(quiz -> paraResumo(quiz, aprovacoes, nomes, melhores, papel, usuarioId))
                .toList();
    }

    private QuizResumoResponseDTO paraResumo(QuizEntity quiz,
                                             List<QuizAprovacaoEntity> aprovacoes,
                                             Map<UUID, String> nomes,
                                             Map<UUID, NotaDTO> melhores,
                                             PapelUsuarioEnum papel,
                                             UUID usuarioId) {

        var doQuiz = aprovacoes.stream()
                .filter(aprovacao -> aprovacao.getQuizId().equals(quiz.getId()))
                .toList();

        boolean administrador = papel == PapelUsuarioEnum.ADMINISTRADOR;
        boolean autor = quiz.getAutorId().equals(usuarioId);

        return new QuizResumoResponseDTO(
                quiz.getId(),
                quiz.getTitulo(),
                quiz.getDescricao(),
                new OpcaoDTO(quiz.getCategoria().name(), quiz.getCategoria().getDescricao()),
                new OpcaoDTO(quiz.getNivel().name(), quiz.getNivel().getDescricao()),
                nomes.get(quiz.getAutorId()),
                quiz.getStatus(),
                (autor || administrador) ? quiz.getMotivoRejeicao() : null,
                doQuiz.size(),
                APROVACOES_DE_PROFESSORES_PARA_PUBLICAR,
                autor || administrador,
                podeAprovar(quiz, doQuiz, papel, usuarioId),
                administrador ? doQuiz.stream().map(a -> nomes.get(a.getProfessorId())).toList() : List.of(),
                quiz.getQuestoes().size(),
                NOTA_MINIMA_PARA_CONCLUIR,
                melhores.get(quiz.getId()),
                quiz.getCriadoEm()
        );
    }

    private boolean podeAprovar(QuizEntity quiz,
                                List<QuizAprovacaoEntity> aprovacoes,
                                PapelUsuarioEnum papel,
                                UUID usuarioId) {

        return papel == PapelUsuarioEnum.PROFESSOR
                && !quiz.getAutorId().equals(usuarioId)
                && quiz.getStatus() == StatusAprovacaoEnum.PENDENTE
                && aprovacoes.stream().noneMatch(aprovacao -> aprovacao.getProfessorId().equals(usuarioId));
    }

    private boolean gabarito(QuizEntity quiz, PapelUsuarioEnum papel, UUID usuarioId) {
        if (papel == PapelUsuarioEnum.ADMINISTRADOR) {
            return true;
        }

        if (quiz.getAutorId().equals(usuarioId)) {
            return true;
        }

        return papel == PapelUsuarioEnum.PROFESSOR
                && quiz.getStatus() == StatusAprovacaoEnum.PENDENTE;
    }

    private List<ArtigoSugeridoDTO> artigosSugeridos(QuizEntity quiz) {
        if (quiz.getArtigoIds().isEmpty()) {
            return List.of();
        }

        var publicados = artigoRepository.findAllById(quiz.getArtigoIds()).stream()
                .filter(artigo -> PUBLICADOS.contains(artigo.getStatus()))
                .collect(Collectors.toMap(ArtigoEntity::getId, ArtigoEntity::getTitulo));

        return quiz.getArtigoIds().stream()
                .filter(publicados::containsKey)
                .map(artigoId -> new ArtigoSugeridoDTO(artigoId, publicados.get(artigoId)))
                .toList();
    }

    private List<QuestaoResponseDTO> questoes(QuizEntity quiz, boolean gabarito) {
        return quiz.getQuestoes().stream()
                .map(questao -> new QuestaoResponseDTO(
                        questao.getId(),
                        questao.getEnunciado(),
                        gabarito ? questao.getExplicacao() : null,
                        alternativas(questao, gabarito)))
                .toList();
    }

    private List<AlternativaResponseDTO> alternativas(QuizQuestaoEntity questao, boolean gabarito) {
        return questao.getAlternativas().stream()
                .map(alternativa -> new AlternativaResponseDTO(
                        alternativa.getId(),
                        alternativa.getTexto(),
                        gabarito ? alternativa.isCorreta() : null))
                .toList();
    }

    private Map<UUID, NotaDTO> melhoresTentativas(List<UUID> quizzesIds, UUID usuarioId) {
        if (usuarioId == null) {
            return Map.of();
        }

        return quizTentativaRepository.findByUsuarioIdAndQuizIdIn(usuarioId, quizzesIds).stream()
                .sorted(Comparator.comparing(QuizTentativaEntity::getFeitaEm))
                .collect(Collectors.toMap(
                        QuizTentativaEntity::getQuizId,
                        this::paraNota,
                        this::melhorEntre));
    }

    private NotaDTO melhorEntre(NotaDTO atual, NotaDTO candidata) {
        return candidata.percentual() >= atual.percentual() ? candidata : atual;
    }

    private NotaDTO paraNota(QuizTentativaEntity tentativa) {
        int percentual = (int) Math.round(tentativa.getAcertos() * 100.0 / tentativa.getTotal());

        return new NotaDTO(
                tentativa.getAcertos(),
                tentativa.getTotal(),
                percentual,
                percentual >= NOTA_MINIMA_PARA_CONCLUIR);
    }

    private Map<UUID, String> buscarNomes(List<QuizEntity> quizzes, List<QuizAprovacaoEntity> aprovacoes) {
        var ids = new HashSet<UUID>();

        quizzes.forEach(quiz -> ids.add(quiz.getAutorId()));
        aprovacoes.forEach(aprovacao -> ids.add(aprovacao.getProfessorId()));

        return usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UsuarioEntity::getId, UsuarioEntity::getNomeCompleto));
    }

    private UUID usuarioAutenticado() {
        var jwt = jwtAutenticado();

        if (jwt == null) {
            return null;
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException excecao) {
            return null;
        }
    }

    private PapelUsuarioEnum papelAutenticado() {
        var jwt = jwtAutenticado();

        if (jwt == null) {
            return null;
        }

        try {
            return PapelUsuarioEnum.valueOf(jwt.getClaimAsString("papel"));
        } catch (IllegalArgumentException | NullPointerException excecao) {
            return null;
        }
    }

    private Jwt jwtAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        return jwt;
    }
}
