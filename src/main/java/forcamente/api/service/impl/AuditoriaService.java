package forcamente.api.service.impl;

import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.PaginaDTO;
import forcamente.api.dto.RegistroAuditoriaResponseDTO;
import forcamente.api.entity.RegistroAuditoriaEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.repository.IRegistroAuditoriaRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IAuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService implements IAuditoriaService {

    private static final LocalDateTime INICIO_PADRAO = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final IRegistroAuditoriaRepository registroAuditoriaRepository;

    private final IUsuarioRepository usuarioRepository;

    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AcaoAuditoriaEnum acao, RecursoAuditoriaEnum recursoTipo, UUID recursoId) {
        gravar(acao, recursoTipo, recursoId, usuarioAutenticado());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AcaoAuditoriaEnum acao, RecursoAuditoriaEnum recursoTipo, UUID recursoId, UUID usuarioId) {
        gravar(acao, recursoTipo, recursoId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<RegistroAuditoriaResponseDTO> consultar(
            LocalDate de, LocalDate ate, AcaoAuditoriaEnum acao, int pagina, int tamanho) {

        log.info("consultar auditoria: de={} ate={} acao={}", de, ate, acao);

        LocalDateTime inicio = de == null ? INICIO_PADRAO : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.now(clock) : ate.atTime(LocalTime.MAX);
        Pageable paginacao = PageRequest.of(pagina, tamanho);

        Page<RegistroAuditoriaEntity> resultado = acao == null
                ? registroAuditoriaRepository
                        .findByOcorridoEmBetweenOrderByOcorridoEmDesc(inicio, fim, paginacao)
                : registroAuditoriaRepository
                        .findByOcorridoEmBetweenAndAcaoOrderByOcorridoEmDesc(inicio, fim, acao, paginacao);

        List<RegistroAuditoriaResponseDTO> conteudo = paraDTO(resultado.getContent());

        return new PaginaDTO<>(
                conteudo,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Override
    public List<OpcaoDTO> listarAcoes() {
        log.info("listarAcoes");
        return Arrays.stream(AcaoAuditoriaEnum.values())
                .map(acao -> new OpcaoDTO(acao.name(), acao.getDescricao()))
                .toList();
    }

    @Override
    public List<OpcaoDTO> listarRecursos() {
        log.info("listarRecursos");
        return Arrays.stream(RecursoAuditoriaEnum.values())
                .map(recurso -> new OpcaoDTO(recurso.name(), recurso.getDescricao()))
                .toList();
    }

    private List<RegistroAuditoriaResponseDTO> paraDTO(List<RegistroAuditoriaEntity> registros) {
        Map<UUID, UsuarioEntity> usuarios = buscarUsuarios(registros);

        return registros.stream()
                .map(registro -> {
                    UsuarioEntity usuario = registro.getUsuarioId() == null
                            ? null
                            : usuarios.get(registro.getUsuarioId());

                    return new RegistroAuditoriaResponseDTO(
                            registro.getId(),
                            registro.getOcorridoEm(),
                            registro.getAcao(),
                            registro.getAcao().getDescricao(),
                            registro.getRecursoTipo(),
                            registro.getRecursoTipo() == null ? null : registro.getRecursoTipo().getDescricao(),
                            registro.getRecursoId(),
                            registro.getUsuarioId(),
                            usuario == null ? null : usuario.getNomeCompleto(),
                            usuario == null ? null : usuario.getEmail());
                })
                .toList();
    }

    private Map<UUID, UsuarioEntity> buscarUsuarios(List<RegistroAuditoriaEntity> registros) {
        List<UUID> ids = registros.stream()
                .map(RegistroAuditoriaEntity::getUsuarioId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return usuarioRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(UsuarioEntity::getId, Function.identity()));
    }

    private void gravar(AcaoAuditoriaEnum acao, RecursoAuditoriaEnum recursoTipo, UUID recursoId, UUID usuarioId) {
        var registro = new RegistroAuditoriaEntity();
        registro.setOcorridoEm(LocalDateTime.now(clock));
        registro.setAcao(acao);
        registro.setRecursoTipo(recursoTipo);
        registro.setRecursoId(recursoId);
        registro.setUsuarioId(usuarioId);

        registroAuditoriaRepository.save(registro);

        log.info("Auditoria: acao={} recurso={} recursoId={} usuario={}",
                acao, recursoTipo, recursoId, usuarioId);
    }

    private UUID usuarioAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException excecao) {
            return null;
        }
    }
}
