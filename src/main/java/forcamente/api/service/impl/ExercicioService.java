package forcamente.api.service.impl;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return paraDTO(visiveis(papel, usuarioId), papel, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExercicioResponseDTO> listarPorGrupoMuscular(GrupoMuscularEnum grupoMuscular) {
        log.info("listarPorGrupoMuscular: {}", grupoMuscular);

        PapelUsuarioEnum papel = papelAutenticado();
        UUID usuarioId = usuarioAutenticado();

        var doGrupo = visiveis(papel, usuarioId).stream()
                .filter(exercicio -> exercicio.getGrupoMuscular() == grupoMuscular)
                .toList();

        return paraDTO(doGrupo, papel, usuarioId);
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

        ExercicioEntity exercicioEntity = findById(exercicioId);

        if (exercicioEntity.getGrupoMuscular() != exercicioRequestDTO.grupoMuscular()) {
            throw new ConflitoException("O Grupo Muscular não pode ser alterado");
        }

        if (exercicioRepository.existsByNomeIgnoreCaseAndIdNot(exercicioRequestDTO.nome(), exercicioId)) {
            throw new ConflitoException("Ja existe um exercicio cadastrado com o nome: " +
                    exercicioRequestDTO.nome());
        }

        exercicioMapper.atualizarEntity(exercicioRequestDTO, exercicioEntity);
        ExercicioEntity exercicioSalvo = exercicioRepository.save(exercicioEntity);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.EXERCICIO, exercicioSalvo.getId());

        return paraDTO(List.of(exercicioSalvo), papelAutenticado(), usuarioAutenticado()).getFirst();
    }

    @Override
    @Transactional
    public void excluirExercicio(UUID exercicioId) {
        log.info("excluirExercicio: {}", exercicioId);
        exercicioRepository.delete(findById(exercicioId));

        auditoriaService.registrar(
                AcaoAuditoriaEnum.EXCLUIDO, RecursoAuditoriaEnum.EXERCICIO, exercicioId);
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

    private List<ExercicioEntity> visiveis(PapelUsuarioEnum papel, UUID usuarioId) {
        return exercicioRepository.buscarVisiveis(
                statusVisiveis(papel), StatusAprovacaoEnum.REJEITADO, usuarioId);
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
