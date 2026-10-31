package forcamente.api.service.impl;

import forcamente.api.dto.AnonimizadorPendenteDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.repository.IQuizTentativaRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IAnonimizacaoService;
import forcamente.api.service.IAuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnonimizacaoService implements IAnonimizacaoService {

    private static final String NOME_ANONIMO = "Usuario anonimizado";

    private final IUsuarioRepository usuarioRepository;

    private final IQuizTentativaRepository quizTentativaRepository;

    private final PasswordEncoder passwordEncoder;

    private final Clock clock;

    private final IAuditoriaService auditoriaService;

    @Override
    @Transactional
    public void solicitar(UUID usuarioId) {
        UsuarioEntity usuario = buscar(usuarioId);

        if (usuario.getAnonimizadoEm() != null) {
            throw new ConflitoException("Esta conta ja foi anonimizada");
        }

        if (usuario.getPapel() == PapelUsuarioEnum.ADMINISTRADOR) {
            throw new RegraDeNegocioException(
                    "Conta de administrador nao pode ser excluida: a plataforma ficaria sem quem "
                            + "cumpre os pedidos de exclusao das outras pessoas");
        }


        if (usuario.getAnonimizacaoSolicitadaEm() != null) {
            log.info("Anonimizacao ja solicitada para: usuario={}", usuarioId);
            return;
        }

        usuario.setAnonimizacaoSolicitadaEm(LocalDateTime.now(clock));
        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, usuarioId);

        log.info("Anonimizacao solicitada: usuario={}", usuarioId);
    }

    @Override
    @Transactional
    public void cancelarSolicitacao(UUID usuarioId) {
        UsuarioEntity usuario = buscar(usuarioId);

        if (usuario.getAnonimizadoEm() != null) {
            throw new ConflitoException("Esta conta ja foi anonimizada");
        }

        if (usuario.getAnonimizacaoSolicitadaEm() == null) {
            throw new RegraDeNegocioException("Nao existe pedido de exclusao para cancelar");
        }

        usuario.setAnonimizacaoSolicitadaEm(null);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, usuarioId);

        log.info("Pedido de anonimizacao cancelado: usuario={}", usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnonimizadorPendenteDTO> listarPendentes() {
        log.info("listar pendentes");

        return usuarioRepository.findByAnonimizacaoSolicitadaEmIsNotNullAndAnonimizadoEmIsNullOrderByAnonimizacaoSolicitadaEm()
                .stream()
                .map(usuario -> new AnonimizadorPendenteDTO(usuario.getId(),usuario.getNomeCompleto(),usuario.getEmail(),usuario.getPapel(),usuario.getAnonimizacaoSolicitadaEm())
                ).toList();
    }

    @Override
    @Transactional
    public void anonimizar (UUID usuarioId) {
        UsuarioEntity usuario = buscar(usuarioId);

        if (usuario.getAnonimizadoEm() != null) {
            throw new ConflitoException("Esta conta ja foi anonimizada");
        }

        if (usuario.getPapel() == PapelUsuarioEnum.ADMINISTRADOR) {
            throw new RegraDeNegocioException(
                    "Conta de administrador nao pode ser excluida: a plataforma ficaria sem quem "
                            + "cumpre os pedidos de exclusao das outras pessoas");
        }


        if (usuario.getAnonimizacaoSolicitadaEm() == null) {
            throw new RegraDeNegocioException("Este usuario nao solicitou anonimizacao");
        }

        usuario.setNomeCompleto(NOME_ANONIMO);
        usuario.setEmail("anonimizado-" + usuario.getId() + "@forcamente.invalid");
        usuario.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setDataNascimento(null);

        usuario.setCref(null);
        usuario.setFormacao(null);
        usuario.setSituacaoCref(null);
        usuario.setMotivoRejeicaoCref(null);
        usuario.setCrefReenvios(null);
        usuario.setCrefReenvioDia(null);

        usuario.setCep(null);
        usuario.setLogradouro(null);
        usuario.setNumero(null);
        usuario.setComplemento(null);
        usuario.setBairro(null);
        usuario.setCidade(null);
        usuario.setEstado(null);

        limparCodigos(usuario);

        quizTentativaRepository.deleteByUsuarioId(usuario.getId());

        usuario.setAtivo(false);
        usuario.setAnonimizadoEm(LocalDateTime.now(clock));

        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ANONIMIZADO, RecursoAuditoriaEnum.USUARIO, usuarioId);

        log.info("Usuario anonimizado: {}", usuarioId);
    }

    private void limparCodigos(UsuarioEntity usuario) {
        usuario.setCodigoHash(null);
        usuario.setCodigoTipo(null);
        usuario.setCodigoExpiraEm(null);
        usuario.setCodigoTentativas(null);

        usuario.setMfaPedidos(null);
        usuario.setMfaJanelaInicio(null);
        usuario.setMfaBloqueadoAte(null);
        usuario.setMfaOcorrenciasBloqueio(null);

        usuario.setRedefinicaoPedidos(null);
        usuario.setRedefinicaoDia(null);
        usuario.setRedefinicaoBloqueadoAte(null);
        usuario.setRedefinicaoErrosConsecutivos(null);
    }

    private UsuarioEntity buscar(UUID usuarioID) {
        return usuarioRepository.findById(usuarioID)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao econtrado:" + usuarioID));
    }
}
