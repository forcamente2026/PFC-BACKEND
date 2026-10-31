package forcamente.api.service.impl;

import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.AlternativaRequestDTO;
import forcamente.api.dto.AlternativaResponseDTO;
import forcamente.api.dto.ArtigoSugeridoDTO;
import forcamente.api.dto.NotaDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.QuestaoRequestDTO;
import forcamente.api.dto.QuestaoResponseDTO;
import forcamente.api.dto.QuizDetalheResponseDTO;
import forcamente.api.dto.QuizEdicaoResponseDTO;
import forcamente.api.dto.QuizRequestDTO;
import forcamente.api.dto.QuizResumoResponseDTO;
import forcamente.api.entity.ArtigoEntity;
import forcamente.api.entity.QuizAlternativaEntity;
import forcamente.api.entity.QuizAprovacaoEntity;
import forcamente.api.entity.QuizEntity;
import forcamente.api.entity.QuizQuestaoEntity;
import forcamente.api.entity.QuizTentativaEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.repository.IArtigoRepository;
import forcamente.api.repository.IQuizAprovacaoRepository;
import forcamente.api.repository.IQuizRepository;
import forcamente.api.repository.IQuizTentativaRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IAuditoriaService;
import forcamente.api.service.IQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
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

    private final IAuditoriaService auditoriaService;

    private final Clock clock;

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

        return paraDetalhe(quiz, papel, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizEdicaoResponseDTO buscarParaEdicao(UUID quizId) {
        log.info("buscarParaEdicao: {}", quizId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz nao encontrado: " + quizId));

        exigirAutorOuAdministrador(quiz, papel, usuarioId);

        return new QuizEdicaoResponseDTO(
                quiz.getId(),
                quiz.getTitulo(),
                quiz.getDescricao(),
                quiz.getCategoria(),
                quiz.getNivel(),
                List.copyOf(quiz.getArtigoIds()),
                quiz.getQuestoes().stream().map(this::paraQuestaoRequest).toList()
        );
    }

    @Override
    @Transactional
    public QuizDetalheResponseDTO criarQuiz(QuizRequestDTO quizRequestDTO) {
        log.info("criarQuiz: {}", quizRequestDTO.titulo());

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quiz = new QuizEntity();

        quiz.setAutorId(usuarioId);
        quiz.setCriadoEm(LocalDateTime.now(clock));
        quiz.setStatus(papel == PapelUsuarioEnum.ADMINISTRADOR
                ? StatusAprovacaoEnum.APROVADO
                : StatusAprovacaoEnum.PENDENTE);

        aplicar(quizRequestDTO, quiz);

        var salvo = quizRepository.save(quiz);

        auditoriaService.registrar(AcaoAuditoriaEnum.CRIADO, RecursoAuditoriaEnum.QUIZ, salvo.getId());

        return paraDetalhe(salvo, papel, usuarioId);
    }

    @Override
    @Transactional
    public QuizDetalheResponseDTO atualizarQuiz(UUID quizId, QuizRequestDTO quizRequestDTO) {
        log.info("atualizarQuiz: {}", quizId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz nao encontrado: " + quizId));

        exigirAutorOuAdministrador(quiz, papel, usuarioId);

        aplicar(quizRequestDTO, quiz);

        quiz.setAtualizadoEm(LocalDateTime.now(clock));

        if (papel != PapelUsuarioEnum.ADMINISTRADOR) {
            quiz.setStatus(StatusAprovacaoEnum.PENDENTE);
            quiz.setMotivoRejeicao(null);
            quizAprovacaoRepository.deleteByQuizId(quizId);
        }

        var salvo = quizRepository.save(quiz);

        auditoriaService.registrar(AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.QUIZ, quizId);

        return paraDetalhe(salvo, papel, usuarioId);
    }

    @Override
    @Transactional
    public void excluirQuiz(UUID quizId) {
        log.info("excluirQuiz: {}", quizId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz nao encontrado: " + quizId));

        exigirAutorOuAdministrador(quiz, papel, usuarioId);

        quizRepository.delete(quiz);

        auditoriaService.registrar(AcaoAuditoriaEnum.EXCLUIDO, RecursoAuditoriaEnum.QUIZ, quizId);
    }

    @Override
    @Transactional
    public QuizDetalheResponseDTO aprovarComoProfessor(UUID quizId) {
        log.info("aprovarComoProfessor: {}", quizId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz nao encontrado: " + quizId));

        if (quiz.getAutorId().equals(usuarioId)) {
            throw new AccessDeniedException("Acesso negado");
        }

        if (quiz.getStatus() != StatusAprovacaoEnum.PENDENTE) {
            throw new ConflitoException("Só é possível aprovar um quiz que está pendente");
        }

        if (quizAprovacaoRepository.existsByQuizIdAndProfessorId(quizId, usuarioId)) {
            throw new ConflitoException("Você já aprovou este quiz");
        }

        var aprovacao = new QuizAprovacaoEntity();
        aprovacao.setQuizId(quizId);
        aprovacao.setProfessorId(usuarioId);
        aprovacao.setAprovadoEm(LocalDateTime.now(clock));

        quizAprovacaoRepository.save(aprovacao);

        auditoriaService.registrar(AcaoAuditoriaEnum.APROVADO, RecursoAuditoriaEnum.QUIZ, quizId);

        if (quizAprovacaoRepository.countByQuizId(quizId) >= APROVACOES_DE_PROFESSORES_PARA_PUBLICAR) {
            quiz.setStatus(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
            quizRepository.save(quiz);
        }

        return paraDetalhe(quiz, papel, usuarioId);
    }

    @Override
    @Transactional
    public QuizDetalheResponseDTO decidirSituacao(UUID quizId,
                                                  AlterarStatusRequestDTO alterarStatusRequestDTO) {
        log.info("decidirSituacao: {} para {}", quizId, alterarStatusRequestDTO.status());

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quiz nao encontrado: " + quizId));

        if (quiz.getStatus() != StatusAprovacaoEnum.PENDENTE
                && quiz.getStatus() != StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES) {

            throw new ConflitoException("Este quiz já foi decidido");
        }

        boolean rejeitado = alterarStatusRequestDTO.status() == StatusAprovacaoEnum.REJEITADO;

        quiz.setStatus(alterarStatusRequestDTO.status());
        quiz.setMotivoRejeicao(rejeitado ? alterarStatusRequestDTO.motivo().trim() : null);

        var salvo = quizRepository.save(quiz);

        auditoriaService.registrar(
                rejeitado ? AcaoAuditoriaEnum.REJEITADO : AcaoAuditoriaEnum.APROVADO,
                RecursoAuditoriaEnum.QUIZ, quizId);

        return paraDetalhe(salvo, papel, usuarioId);
    }

    private void aplicar(QuizRequestDTO quizRequestDTO, QuizEntity quiz) {
        quiz.setTitulo(quizRequestDTO.titulo());
        quiz.setDescricao(quizRequestDTO.descricao());
        quiz.setCategoria(quizRequestDTO.categoria());
        quiz.setNivel(quizRequestDTO.nivel());

        var artigos = artigosValidados(quizRequestDTO.artigos());

        quiz.getArtigoIds().clear();
        quiz.getArtigoIds().addAll(artigos);

        quiz.getQuestoes().clear();
        quiz.getQuestoes().addAll(questoesDe(quizRequestDTO.questoes()));
    }

    private List<UUID> artigosValidados(List<UUID> artigos) {
        if (artigos == null || artigos.isEmpty()) {
            return List.of();
        }

        if (new HashSet<>(artigos).size() != artigos.size()) {
            throw new RegraDeNegocioException("Nao repita o mesmo artigo na leitura sugerida");
        }

        var publicados = artigoRepository.findAllById(artigos).stream()
                .filter(artigo -> PUBLICADOS.contains(artigo.getStatus()))
                .map(ArtigoEntity::getId)
                .collect(Collectors.toSet());

        var invalido = artigos.stream().filter(artigoId -> !publicados.contains(artigoId)).findFirst();

        if (invalido.isPresent()) {
            throw new RegraDeNegocioException(
                    "Artigo indicado nao existe ou nao esta publicado: " + invalido.get());
        }

        return artigos;
    }

    private List<QuizQuestaoEntity> questoesDe(List<QuestaoRequestDTO> questoes) {
        return questoes.stream().map(pedido -> {
            var questao = new QuizQuestaoEntity();

            questao.setEnunciado(pedido.enunciado());
            questao.setExplicacao(pedido.explicacao());
            questao.getAlternativas().addAll(alternativasDe(pedido.alternativas()));

            return questao;
        }).toList();
    }

    private List<QuizAlternativaEntity> alternativasDe(List<AlternativaRequestDTO> alternativas) {
        return alternativas.stream().map(pedido -> {
            var alternativa = new QuizAlternativaEntity();

            alternativa.setTexto(pedido.texto());
            alternativa.setCorreta(pedido.correta());

            return alternativa;
        }).toList();
    }

    private QuestaoRequestDTO paraQuestaoRequest(QuizQuestaoEntity questao) {
        var alternativas = questao.getAlternativas().stream()
                .map(alternativa -> new AlternativaRequestDTO(alternativa.getTexto(), alternativa.isCorreta()))
                .toList();

        return new QuestaoRequestDTO(questao.getEnunciado(), questao.getExplicacao(), alternativas);
    }

    private QuizDetalheResponseDTO paraDetalhe(QuizEntity quiz, PapelUsuarioEnum papel, UUID usuarioId) {
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

    private void exigirAutorOuAdministrador(QuizEntity quiz, PapelUsuarioEnum papel, UUID usuarioId) {
        if (papel == PapelUsuarioEnum.ADMINISTRADOR) {
            return;
        }

        if (!quiz.getAutorId().equals(usuarioId)) {
            throw new AccessDeniedException("Acesso negado");
        }
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
