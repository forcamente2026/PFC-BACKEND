package forcamente.api.service.impl;

import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.ExercicioRequestDTO;
import forcamente.api.dto.ExercicioResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.entity.ExercicioAprovacaoEntity;
import forcamente.api.entity.ExercicioEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.mapper.ExercicioMapper;
import forcamente.api.repository.IExercicioAprovacaoRepository;
import forcamente.api.repository.IExercicioRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IAuditoriaService;
import forcamente.api.service.IExercicioService;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExercicioService implements IExercicioService {

    private static final int APROVACOES_DE_PROFESSORES_PARA_PUBLICAR = 3;

    private final IExercicioRepository exercicioRepository;

    private final IExercicioAprovacaoRepository exercicioAprovacaoRepository;

    private final IUsuarioRepository usuarioRepository;

    private final ExercicioMapper exercicioMapper;

    private final IAuditoriaService auditoriaService;

    private final Clock clock;

    @Override
    @Transactional
    public ExercicioResponseDTO criarExercicio(ExercicioRequestDTO exercicioRequestDTO) {
        log.info("criarExercicio: {}", exercicioRequestDTO.nome());

        if (exercicioRepository.existsByNomeIgnoreCase(exercicioRequestDTO.nome())) {
            throw new ConflitoException(
                    "Ja existe um exercicio cadastrado com o nome: " + exercicioRequestDTO.nome());
        }

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        var exercicioEntity = exercicioMapper.toEntity(exercicioRequestDTO);

        exercicioEntity.setAutorId(usuarioId);
        exercicioEntity.setStatus(papel == PapelUsuarioEnum.ADMINISTRADOR
                ? StatusAprovacaoEnum.APROVADO
                : StatusAprovacaoEnum.PENDENTE);

        var exercicioSalvo = exercicioRepository.save(exercicioEntity);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.CRIADO, RecursoAuditoriaEnum.EXERCICIO, exercicioSalvo.getId());

        return paraDTO(List.of(exercicioSalvo), papel, usuarioId).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExercicioResponseDTO> listarExercicios() {
        log.info("listarExercicios");

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        return paraDTO(visiveis(papel, usuarioId, null), papel, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExercicioResponseDTO> listarPorGrupoMuscular(GrupoMuscularEnum grupoMuscular) {
        log.info("listarPorGrupoMuscular: {}", grupoMuscular);

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        return paraDTO(visiveis(papel, usuarioId, grupoMuscular), papel, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public ExercicioResponseDTO buscarPorId(UUID exercicioId) {
        log.info("buscarPorId: {}", exercicioId);

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        ExercicioEntity exercicio = findById(exercicioId);

        if (!podeVer(exercicio, papel, usuarioId)) {
            throw new RecursoNaoEncontradoException("Exercicio nao encontrado: " + exercicioId);
        }

        return paraDTO(List.of(exercicio), papel, usuarioId).getFirst();
    }

    @Override
    @Transactional
    public ExercicioResponseDTO atualizarExercicio(UUID exercicioId, ExercicioRequestDTO exercicioRequestDTO) {
        log.info("atualizarExercicio:{}", exercicioId);

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        ExercicioEntity exercicioEntity = findById(exercicioId);

        exigirAutorOuAdministrador(exercicioEntity, papel, usuarioId);

        if (exercicioEntity.getGrupoMuscular() != exercicioRequestDTO.grupoMuscular()) {
            throw new ConflitoException("O Grupo Muscular não pode ser alterado");
        }

        if (exercicioRepository.existsByNomeIgnoreCaseAndIdNot(exercicioRequestDTO.nome(), exercicioId)) {
            throw new ConflitoException("Ja existe um exercicio cadastrado com o nome: " +
                    exercicioRequestDTO.nome());
        }

        exercicioMapper.atualizarEntity(exercicioRequestDTO, exercicioEntity);

        if (papel != PapelUsuarioEnum.ADMINISTRADOR) {
            exercicioEntity.setStatus(StatusAprovacaoEnum.PENDENTE);
            exercicioEntity.setMotivoRejeicao(null);
            exercicioAprovacaoRepository.deleteByExercicioId(exercicioId);
        }

        ExercicioEntity exercicioSalvo = exercicioRepository.save(exercicioEntity);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.EXERCICIO, exercicioSalvo.getId());

        return paraDTO(List.of(exercicioSalvo), papel, usuarioId).getFirst();
    }

    @Override
    @Transactional
    public void excluirExercicio(UUID exercicioId) {
        log.info("excluirExercicio: {}", exercicioId);

        ExercicioEntity exercicio = findById(exercicioId);

        exigirAutorOuAdministrador(exercicio, papelAutenticado(), usuarioAutenticado());

        exercicioRepository.delete(exercicio);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.EXCLUIDO, RecursoAuditoriaEnum.EXERCICIO, exercicioId);
    }

    private ExercicioEntity buscarParaEscrita(UUID exercicioId) {
        return exercicioRepository.buscarParaEscrita(exercicioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Exercicio nao encontrado: " + exercicioId));
    }

    private ExercicioEntity findById(UUID exercicioId) {
        return exercicioRepository.findById(exercicioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Exercicio nao encontrado: " + exercicioId));
    }

    @Override
    public List<OpcaoDTO> listarGruposMusculares() {
        log.info("listarGruposMusculares");
        return Arrays.stream(GrupoMuscularEnum.values())
                .map(grupo -> new OpcaoDTO(grupo.name(), grupo.getDescricao()))
                .toList();
    }

    @Override
    public List<OpcaoDTO> listarNiveis() {
        log.info("listarNiveis");
        return Arrays.stream(NivelDificuldadeEnum.values())
                .map(nivel -> new OpcaoDTO(nivel.name(), nivel.getDescricao()))
                .toList();
    }

    @Override
    @Transactional
    public ExercicioResponseDTO aprovarComoProfessor(UUID exercicioId) {
        log.info("aprovarComoProfessor: {}", exercicioId);

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        ExercicioEntity exercicio = buscarParaEscrita(exercicioId);

        if (ehOAutor(exercicio, usuarioId)) {
            throw new AccessDeniedException("Acesso negado");
        }

        if (exercicio.getStatus() != StatusAprovacaoEnum.PENDENTE) {
            throw new ConflitoException("Só é possível aprovar um exercício que está pendente");
        }

        if (exercicioAprovacaoRepository.existsByExercicioIdAndProfessorId(exercicioId, usuarioId)) {
            throw new ConflitoException("Você já aprovou este exercício");
        }

        var aprovacao = new ExercicioAprovacaoEntity();
        aprovacao.setExercicioId(exercicioId);
        aprovacao.setProfessorId(usuarioId);
        aprovacao.setAprovadoEm(LocalDateTime.now(clock));

        exercicioAprovacaoRepository.save(aprovacao);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.APROVADO, RecursoAuditoriaEnum.EXERCICIO, exercicioId);

        if (exercicioAprovacaoRepository.countByExercicioId(exercicioId)
                >= APROVACOES_DE_PROFESSORES_PARA_PUBLICAR) {

            exercicio.setStatus(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
            exercicioRepository.save(exercicio);
        }

        return paraDTO(List.of(exercicio), papel, usuarioId).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExercicioResponseDTO> listarPendentes() {
        log.info("listarPendentes");

        var pendentes = exercicioRepository.findByStatusInOrderByCriadoEmAsc(
                List.of(StatusAprovacaoEnum.PENDENTE, StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES));

        return paraDTO(pendentes, papelAutenticado(), usuarioAutenticado());
    }

    @Override
    @Transactional
    public ExercicioResponseDTO decidirSituacao(UUID exercicioId,
                                                AlterarStatusRequestDTO alterarStatusRequestDTO) {

        log.info("decidirSituacao: {} para {}", exercicioId, alterarStatusRequestDTO.status());

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        ExercicioEntity exercicio = buscarParaEscrita(exercicioId);

        if (exercicio.getStatus() != StatusAprovacaoEnum.PENDENTE
                && exercicio.getStatus() != StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES) {

            throw new ConflitoException("Este exercício já foi decidido");
        }

        boolean rejeitado = alterarStatusRequestDTO.status() == StatusAprovacaoEnum.REJEITADO;

        exercicio.setStatus(alterarStatusRequestDTO.status());
        exercicio.setMotivoRejeicao(rejeitado ? alterarStatusRequestDTO.motivo().trim() : null);

        ExercicioEntity salvo = exercicioRepository.save(exercicio);

        auditoriaService.registrar(
                rejeitado ? AcaoAuditoriaEnum.REJEITADO : AcaoAuditoriaEnum.APROVADO,
                RecursoAuditoriaEnum.EXERCICIO, exercicioId);

        return paraDTO(List.of(salvo), papel, usuarioId).getFirst();
    }

    private void exigirAutorOuAdministrador(ExercicioEntity exercicio,
                                            PapelUsuarioEnum papel,
                                            UUID usuarioId) {

        if (papel == PapelUsuarioEnum.ADMINISTRADOR) {
            return;
        }

        if (!ehOAutor(exercicio, usuarioId)) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private List<ExercicioEntity> visiveis(PapelUsuarioEnum papel,
                                           UUID usuarioId,
                                           GrupoMuscularEnum grupoMuscular) {

        return exercicioRepository.buscarVisiveis(
                statusVisiveis(papel), StatusAprovacaoEnum.REJEITADO, usuarioId, grupoMuscular);
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

    private boolean podeVer(ExercicioEntity exercicio, PapelUsuarioEnum papel, UUID usuarioId) {
        if (statusVisiveis(papel).contains(exercicio.getStatus())) {
            return true;
        }

        return exercicio.getStatus() == StatusAprovacaoEnum.REJEITADO && ehOAutor(exercicio, usuarioId);
    }

    private boolean ehOAutor(ExercicioEntity exercicio, UUID usuarioId) {
        return exercicio.getAutorId() != null && Objects.equals(exercicio.getAutorId(), usuarioId);
    }

    private List<ExercicioResponseDTO> paraDTO(List<ExercicioEntity> exercicios,
                                               PapelUsuarioEnum papel,
                                               UUID usuarioId) {
        if (exercicios.isEmpty()) {
            return List.of();
        }

        var aprovacoes = exercicioAprovacaoRepository.findByExercicioIdIn(
                exercicios.stream().map(ExercicioEntity::getId).toList());

        var nomes = buscarNomes(exercicios, aprovacoes);

        return exercicios.stream()
                .map(exercicio -> paraDTO(exercicio, aprovacoes, nomes, papel, usuarioId))
                .toList();
    }

    private ExercicioResponseDTO paraDTO(ExercicioEntity exercicio,
                                         List<ExercicioAprovacaoEntity> aprovacoes,
                                         Map<UUID, String> nomes,
                                         PapelUsuarioEnum papel,
                                         UUID usuarioId) {

        var doExercicio = aprovacoes.stream()
                .filter(aprovacao -> aprovacao.getExercicioId().equals(exercicio.getId()))
                .toList();

        boolean administrador = papel == PapelUsuarioEnum.ADMINISTRADOR;
        boolean autor = ehOAutor(exercicio, usuarioId);

        return new ExercicioResponseDTO(
                exercicio.getId(),
                exercicio.getNome(),
                exercicio.getGrupoMuscular(),
                exercicio.getNivel(),
                exercicio.getDescricaoExecucao(),
                exercicio.getErrosComuns(),
                exercicio.getAquecimentoRecomendado(),
                exercicio.getEquipamento(),
                exercicio.getGifUrl(),
                exercicio.getVideoYoutubeId(),
                exercicio.getAutorId() == null ? null : nomes.get(exercicio.getAutorId()),
                exercicio.getStatus(),
                (autor || administrador) ? exercicio.getMotivoRejeicao() : null,
                doExercicio.size(),
                APROVACOES_DE_PROFESSORES_PARA_PUBLICAR,
                autor || administrador,
                podeAprovar(exercicio, doExercicio, papel, usuarioId),
                administrador
                        ? doExercicio.stream().map(aprovacao -> nomes.get(aprovacao.getProfessorId())).toList()
                        : List.of(),
                exercicio.getCriadoEm(),
                exercicio.getAtualizadoEm()
        );
    }

    private boolean podeAprovar(ExercicioEntity exercicio,
                                List<ExercicioAprovacaoEntity> aprovacoes,
                                PapelUsuarioEnum papel,
                                UUID usuarioId) {

        return papel == PapelUsuarioEnum.PROFESSOR
                && !ehOAutor(exercicio, usuarioId)
                && exercicio.getStatus() == StatusAprovacaoEnum.PENDENTE
                && aprovacoes.stream()
                        .noneMatch(aprovacao -> aprovacao.getProfessorId().equals(usuarioId));
    }

    private Map<UUID, String> buscarNomes(List<ExercicioEntity> exercicios,
                                          List<ExercicioAprovacaoEntity> aprovacoes) {

        var ids = new HashSet<UUID>();

        exercicios.stream()
                .map(ExercicioEntity::getAutorId)
                .filter(Objects::nonNull)
                .forEach(ids::add);

        aprovacoes.forEach(aprovacao -> ids.add(aprovacao.getProfessorId()));

        if (ids.isEmpty()) {
            return Map.of();
        }

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
