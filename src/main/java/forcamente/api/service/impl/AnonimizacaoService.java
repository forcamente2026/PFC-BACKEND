package forcamente.api.service.impl;

import forcamente.api.dto.AnonimizadorPendenteDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
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

        if (usuario.getAnonimizacaoSolicitadaEm() != null) {
            log.info("Anonimizacao ja solicitada para: usuario={}", usuarioId);
            return;
        }

        usuario.setAnonimizacaoSolicitadaEm(LocalDateTime.now(clock));
        usuarioRepository.save(usuario);

        log.info("Anonimizacao solicitada: usuario={}", usuarioId);
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

        if (usuario.getAnonimizacaoSolicitadaEm() == null) {
            throw new RegraDeNegocioException("Este usuario nao solicitou anonimizacao");
        }

        usuario.setNomeCompleto(NOME_ANONIMO);
        usuario.setEmail("anonimizado-" + usuario.getId() + "@forcamente.invalid");
        usuario.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setCpf(null);
        usuario.setDataNascimento(null);

        usuario.setCref(null);
        usuario.setFormacao(null);
        usuario.setInstituicao(null);

        usuario.setCep(null);
        usuario.setLogradouro(null);
        usuario.setNumero(null);
        usuario.setComplemento(null);
        usuario.setBairro(null);
        usuario.setCidade(null);
        usuario.setEstado(null);

        limparCodigos(usuario);

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
