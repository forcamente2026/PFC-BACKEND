package forcamente.api.service.impl;

import forcamente.api.dto.ArtigoRequestDTO;
import forcamente.api.dto.ArtigoResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.entity.ArtigoAprovacaoEntity;
import forcamente.api.entity.ArtigoEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.*;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.repository.IArtigoAprovacaoRepository;
import forcamente.api.repository.IArtigoRepository;
import forcamente.api.repository.IQuizAprovacaoRepository;
import forcamente.api.repository.IQuizRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IArtigoService;
import forcamente.api.service.IAuditoriaService;
import jdk.jshell.Snippet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import forcamente.api.dto.AlterarStatusRequestDTO;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtigoService implements IArtigoService {

    private static final int APROVACOES_DE_PROFESSORES_PARA_PUBLICAR = 3;

    private static final Set<StatusAprovacaoEnum> PUBLICADOS = Set.of(
            StatusAprovacaoEnum.APROVADO,
            StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);

    private final IArtigoRepository artigoRepository;

    private final IArtigoAprovacaoRepository artigoAprovacaoRepository;

    private final IUsuarioRepository usuarioRepository;

    private final IAuditoriaService auditoriaService;

    private final IQuizRepository quizRepository;

    private final IQuizAprovacaoRepository quizAprovacaoRepository;

    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ArtigoResponseDTO> listarArtigos(StatusAprovacaoEnum status) {
        log.info("listarArtigos: {}", status);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var artigos = artigoRepository.buscarVisiveis(
                statusVisiveis(papel), StatusAprovacaoEnum.REJEITADO,usuarioId, status
        );

        return paraDTO(artigos, papel, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtigoResponseDTO buscarPorId(UUID artigoId) {
        log.info("buscarPorId: {}", artigoId);

        var papel = papelAutenticado();
        var usuarioId= usuarioAutenticado();

        var artigo = artigoRepository.findById(artigoId).filter(encontrado -> podeVer(encontrado, papel, usuarioId)).orElseThrow(() -> new RecursoNaoEncontradoException("Artigo nao encontrado: " + artigoId));

        return paraDTO(List.of(artigo), papel, usuarioId).getFirst();
    }

    @Override
    public List<OpcaoDTO> listarCategorias() {
        log.info("listarCategorias");
        return Arrays.stream(CategoriaArtigoEnum.values()).map(categoria -> new OpcaoDTO(categoria.name(), categoria.getDescricao())).toList();
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

    @Override
    @Transactional
    public ArtigoResponseDTO criarArtigo(ArtigoRequestDTO artigoRequestDTO) {
        log.info("criarArtigo: {}", artigoRequestDTO.titulo());

        var papel = papelAutenticado();
        var usuarioid = usuarioAutenticado();

        exigirAnoNaoFuturo(artigoRequestDTO.ano());

        if (artigoRepository.existsByFonte(fonteNormalizada(artigoRequestDTO))) {
            throw new ConflitoException(
                    "Ja existe um artigo indicando esta fonte: " + fonteNormalizada(artigoRequestDTO));
        }

        var artigo = new ArtigoEntity();
        aplicar(artigoRequestDTO, artigo);
        artigo.setAutorId(usuarioid);
        artigo.setCriadoEm(LocalDateTime.now(clock));
        artigo.setStatus(papel == PapelUsuarioEnum.ADMINISTRADOR ? StatusAprovacaoEnum.APROVADO : StatusAprovacaoEnum.PENDENTE);

        var salvo = artigoRepository.save(artigo);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.CRIADO, RecursoAuditoriaEnum.ARTIGO, salvo.getId()
        );
        return paraDTO(List.of(salvo), papel, usuarioid).getFirst();
    }

    @Override
    @Transactional
    public ArtigoResponseDTO atualizarArtigo(UUID artigoId, ArtigoRequestDTO artigoRequestDTO) {
        log.info("atualizarArtigo: {}", artigoId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var artigo = artigoRepository.findById(artigoId).orElseThrow(() -> new RecursoNaoEncontradoException("Artigo nao encontrado: " + artigoId));

        exigirAutorOuAdministrador(artigo, papel, usuarioId);
        exigirAnoNaoFuturo(artigoRequestDTO.ano());


        if (artigoRepository.existsByFonteAndIdNot(fonteNormalizada(artigoRequestDTO), artigoId)) {
            throw new ConflitoException(
                    "Ja existe um artigo indicando esta fonte: " + fonteNormalizada(artigoRequestDTO));
        }

        aplicar(artigoRequestDTO, artigo);

        if (papel != PapelUsuarioEnum.ADMINISTRADOR) {
            artigo.setStatus(StatusAprovacaoEnum.PENDENTE);
            artigo.setMotivoRejeicao(null);
            artigoAprovacaoRepository.deleteByArtigoId(artigoId);
        }

        var salvo = artigoRepository.save(artigo);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.ARTIGO, salvo.getId()
        );
        return paraDTO(List.of(salvo), papel, usuarioId).getFirst();
    }

    @Override
    @Transactional
    public void excluirArtigo(UUID artigoId) {
        log.info("excluirArtigo: {}", artigoId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var artigo = artigoRepository.findById(artigoId).orElseThrow(() -> new RecursoNaoEncontradoException("Artigo nao encontrado: " + artigoId));

        exigirAutorOuAdministrador(artigo, papel, usuarioId);

        exigirQueNenhumQuizPublicadoIndique(artigo);

        artigoRepository.delete(artigo);

        auditoriaService.registrar(AcaoAuditoriaEnum.EXCLUIDO, RecursoAuditoriaEnum.ARTIGO, artigoId);
    }

    private void exigirQueNenhumQuizPublicadoIndique(ArtigoEntity artigo) {
        var quizzes = quizRepository.buscarPorArtigoIndicado(artigo.getId(), PUBLICADOS);

        if (!quizzes.isEmpty()) {
            throw new ConflitoException(
                    "Este artigo é leitura sugerida do quiz \"" + quizzes.getFirst().getTitulo()
                            + "\". Troque a indicação no quiz antes de excluir"
            );
        }
    }

    private void devolverParaPendenteOsQuizzesQueIndicam(ArtigoEntity artigo) {
        quizRepository.buscarPorArtigoIndicado(artigo.getId(), PUBLICADOS).forEach(quiz -> {
            quiz.setStatus(StatusAprovacaoEnum.PENDENTE);
            quiz.setMotivoRejeicao("O artigo \"" + artigo.getTitulo()
                    + "\", indicado na leitura sugerida, foi rejeitado. Troque a indicação.");

            quizRepository.save(quiz);
            quizAprovacaoRepository.deleteByQuizId(quiz.getId());

            auditoriaService.registrar(
                    AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.QUIZ, quiz.getId()
            );
        });
    }

    @Override
    @Transactional
    public ArtigoResponseDTO aprovarComoProfessor(UUID artigoId) {
        log.info("aprovarComoProfessor: {}", artigoId);

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var artigo = artigoRepository.findById(artigoId).orElseThrow(() -> new RecursoNaoEncontradoException("Artigo nao encontrado: " + artigoId));

        if (artigo.getAutorId().equals(usuarioId)) {
            throw new AccessDeniedException("Acesso negado");
        }

        if (artigo.getStatus() != StatusAprovacaoEnum.PENDENTE) {
            throw new ConflitoException("Só é possível aprovar um artigo que está pendente");
        }

        if (artigoAprovacaoRepository.existsByArtigoIdAndProfessorId(artigoId, usuarioId)) {
            throw new ConflitoException("Você já aprovou este artigo");
        }

        var aprovacao = new ArtigoAprovacaoEntity();
        aprovacao.setArtigoId(artigoId);
        aprovacao.setProfessorId(usuarioId);
        aprovacao.setAprovadoEm(LocalDateTime.now(clock));

        artigoAprovacaoRepository.save(aprovacao);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.APROVADO, RecursoAuditoriaEnum.ARTIGO, artigoId
        );

        if (artigoAprovacaoRepository.countByArtigoId(artigoId) >= APROVACOES_DE_PROFESSORES_PARA_PUBLICAR) {
            artigo.setStatus(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
            artigoRepository.save(artigo);
        }

        return paraDTO(List.of(artigo), papel, usuarioId).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtigoResponseDTO> listarPendentes() {
        log.info("Listar pendentes");

        var artigos = artigoRepository.findByStatusInOrderByCriadoEmAsc(
                Set.of(StatusAprovacaoEnum.PENDENTE,StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES)
        );
        return paraDTO(artigos, papelAutenticado(), usuarioAutenticado());
    }

    @Override
    @Transactional
    public ArtigoResponseDTO decidirSituacao(UUID artigoId, AlterarStatusRequestDTO alterarStatusArtigoRequestDTO) {
        log.info("decidirSituacao: {} para {}", artigoId, alterarStatusArtigoRequestDTO.status());

        var papel = papelAutenticado();
        var usuarioId = usuarioAutenticado();

        var artigo = artigoRepository.findById(artigoId).orElseThrow(() -> new RecursoNaoEncontradoException("Artigo nao encontrado: " + artigoId));

        if (artigo.getStatus() != StatusAprovacaoEnum.PENDENTE && artigo.getStatus() != StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES) {
            throw new ConflitoException(
                    "Este artigo já foi decidido: " + artigo.getStatus()
            );
        }

        boolean rejeitado = alterarStatusArtigoRequestDTO.status() == StatusAprovacaoEnum.REJEITADO;

        if (rejeitado) {
            devolverParaPendenteOsQuizzesQueIndicam(artigo);
        }

        artigo.setStatus(alterarStatusArtigoRequestDTO.status());
        artigo.setMotivoRejeicao(rejeitado ? alterarStatusArtigoRequestDTO.motivo().trim() : null);

        var salvo = artigoRepository.save(artigo);

        auditoriaService.registrar(
                rejeitado ? AcaoAuditoriaEnum.REJEITADO : AcaoAuditoriaEnum.APROVADO, RecursoAuditoriaEnum.ARTIGO, artigoId
        );
        return paraDTO(List.of(salvo), papel, usuarioId).getFirst();
    }

















    private boolean podeVer(ArtigoEntity artigo, PapelUsuarioEnum papel, UUID usuarioId){
        return statusVisiveis(papel).contains(artigo.getStatus()) || (artigo.getStatus() == StatusAprovacaoEnum.REJEITADO && artigo.getAutorId().equals(usuarioId));
    }

    private List<ArtigoResponseDTO> paraDTO(List<ArtigoEntity> artigos, PapelUsuarioEnum papel, UUID usuarioId) {
        if (artigos.isEmpty()) {
            return List.of();
        }

        var aprovacoes = artigoAprovacaoRepository.findByArtigoIdIn(
                artigos.stream().map(ArtigoEntity ::getId).toList()
        );

        var nomes = buscarNomes(artigos, aprovacoes);

        return artigos.stream().map(artigo -> paraDTO(artigo, aprovacoes, nomes, papel, usuarioId)).toList();
    }

    private  ArtigoResponseDTO paraDTO(ArtigoEntity artigo,
                                       List<ArtigoAprovacaoEntity> aprovacoes,
                                       Map<UUID, String> nomes,
                                       PapelUsuarioEnum papel,
                                       UUID usuarioId) {
        var doArtigo = aprovacoes.stream().filter(aprovacao -> aprovacao.getArtigoId().equals(artigo.getId())).toList();

        boolean administrador = papel == PapelUsuarioEnum.ADMINISTRADOR;
        boolean autor = artigo.getAutorId().equals(usuarioId);

        return new ArtigoResponseDTO(
                artigo.getId(),
                artigo.getTitulo(),
                new OpcaoDTO(artigo.getCategoria().name(), artigo.getCategoria().getDescricao()),
                artigo.getResumo(),
                artigo.getFonte(),
                artigo.getAutoria(),
                artigo.getTipoEstudo(),
                artigo.getAno(),
                nomes.get(artigo.getAutorId()),
                artigo.getStatus(),
                (autor || administrador) ? artigo.getMotivoRejeicao() : null,
                doArtigo.size(),
                APROVACOES_DE_PROFESSORES_PARA_PUBLICAR,
                autor || administrador, podeAprovar(artigo, doArtigo, papel, usuarioId),
                administrador ? doArtigo.stream().map(a -> nomes.get(a.getProfessorId())).toList() : List.of(),
                artigo.getCriadoEm()
        );
    }

    private boolean podeAprovar(ArtigoEntity artigo,
                                List<ArtigoAprovacaoEntity> aprovacoes,
                                PapelUsuarioEnum papel,
                                UUID usuarioId) {
        return (papel == PapelUsuarioEnum.PROFESSOR && !artigo.getAutorId().equals(usuarioId) &&
                artigo.getStatus() == StatusAprovacaoEnum.PENDENTE &&
                aprovacoes.stream().noneMatch(aprovacao -> aprovacao.getProfessorId().equals(usuarioId)));

    }

    private Map<UUID, String> buscarNomes(List<ArtigoEntity> artigos, List<ArtigoAprovacaoEntity> aprovacoes){
        var ids = new HashSet<UUID>();
        artigos.forEach(artigo -> ids.add(artigo.getAutorId()));
        aprovacoes.forEach(aprovacao -> ids.add(aprovacao.getProfessorId()));

        return usuarioRepository.findAllById(ids).stream().collect(Collectors.toMap(UsuarioEntity::getId, UsuarioEntity::getNomeCompleto));
    }

    private UUID usuarioAutenticado() {
        var jwt = jwtAutenticado();

        if (jwt == null){
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

        if (jwt == null){
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

    private void exigirAutorOuAdministrador(ArtigoEntity artigo,
                                            PapelUsuarioEnum papel,
                                            UUID usuarioId) {

        if (papel == PapelUsuarioEnum.ADMINISTRADOR) {
            return;
        }

        if (!artigo.getAutorId().equals(usuarioId)) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private void aplicar(ArtigoRequestDTO artigoRequestDTO, ArtigoEntity artigo) {
        artigo.setTitulo(artigoRequestDTO.titulo());
        artigo.setCategoria(artigoRequestDTO.categoria());
        artigo.setResumo(artigoRequestDTO.resumo());
        artigo.setFonte(fonteNormalizada(artigoRequestDTO));
        artigo.setAutoria(artigoRequestDTO.autoria());
        artigo.setTipoEstudo(artigoRequestDTO.tipoEstudo());
        artigo.setAno(artigoRequestDTO.ano());
    }

    private void exigirAnoNaoFuturo(Integer ano) {
        if (ano == null) {
            return;
        }
        int anoVigente = LocalDate.now(clock).getYear();

        if (ano > anoVigente) {
            throw new RegraDeNegocioException("O ano do estudo não pode ser futuro: o máximo é " + anoVigente);
        }
    }


    private String fonteNormalizada(ArtigoRequestDTO artigoRequestDTO) {
        return artigoRequestDTO.fonte().trim();
    }
}

