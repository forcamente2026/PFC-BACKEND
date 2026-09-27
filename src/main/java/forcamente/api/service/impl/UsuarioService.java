package forcamente.api.service.impl;

import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.PaginaDTO;
import forcamente.api.dto.UsuarioAdminResponseDTO;
import forcamente.api.dto.UsuarioAtualizacaoRequestDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.mapper.UsuarioMapper;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IAuditoriaService;
import forcamente.api.service.IDocumentoLegalService;
import forcamente.api.service.IUsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper;

    private final PasswordEncoder passwordEncoder;

    private final Clock clock;

    private final IDocumentoLegalService documentoLegalService;

    private final IAuditoriaService auditoriaService;


    @Override
    @Transactional
    public UsuarioResponseDTO criarUsuario(UsuarioRequestDTO usuarioRequestDTO) {
        log.info("criarUsuario: {}", usuarioRequestDTO.email());

        if (usuarioRequestDTO.papel() == PapelUsuarioEnum.ADMINISTRADOR) {
            throw new RegraDeNegocioException("Administrador nao pode ser criado pelo cadastro publico");
        }

        if (usuarioRepository.existsByEmail(usuarioRequestDTO.email())) {
            throw new ConflitoException(
                    "Ja existe um usuario cadastrado com o e-mail: " + usuarioRequestDTO.email());
        }
        if (usuarioRequestDTO.cref() != null && usuarioRepository.existsByCref(usuarioRequestDTO.cref())) {
            throw new ConflitoException("Ja existe um professor cadastrado com este CREF");
        }

        UsuarioEntity usuarioEntity = usuarioMapper.toEntity(usuarioRequestDTO);

        usuarioEntity.setSenhaHash(passwordEncoder.encode(usuarioRequestDTO.senha()));

        registrarAceites(usuarioEntity);


        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuarioEntity);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.CRIADO, RecursoAuditoriaEnum.USUARIO,
                usuarioSalvo.getId(), usuarioSalvo.getId());

        return usuarioMapper.toDTO(usuarioSalvo);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(UUID usuarioId) {
        log.info("buscarPorId: {}", usuarioId);

        UsuarioEntity usuarioEntity = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario nao encontrado: " + usuarioId));

        auditoriaService.registrar(AcaoAuditoriaEnum.LIDO, RecursoAuditoriaEnum.USUARIO, usuarioId);

        return usuarioMapper.toDTO(usuarioEntity);
    }

    @Override
    public List<OpcaoDTO> listarFormacoes() {
        log.info("listarFormacoes");
        return Arrays.stream(FormacaoEnum.values()).map(formacao -> new OpcaoDTO(formacao.name(), formacao.getDescricao() )).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<UsuarioAdminResponseDTO> listar(
            String busca, PapelUsuarioEnum papel, Boolean ativo, int pagina, int tamanho) {

        log.info("listar usuarios: busca={} papel={} ativo={}", busca, papel, ativo);

        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();

        Page<UsuarioEntity> resultado = usuarioRepository.buscarComFiltros(
                papel, ativo, termo, PageRequest.of(pagina, tamanho));

        auditoriaService.registrar(AcaoAuditoriaEnum.LIDO, RecursoAuditoriaEnum.USUARIO, null);

        return new PaginaDTO<>(
                resultado.getContent().stream().map(usuarioMapper::toAdminDTO).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Override
    @Transactional
    public UsuarioAdminResponseDTO atualizar(UUID usuarioId, UsuarioAtualizacaoRequestDTO dados) {
        log.info("atualizar usuario: {}", usuarioId);

        UsuarioEntity usuario = buscarEntidade(usuarioId);

        if (usuario.getAnonimizadoEm() != null) {
            throw new RegraDeNegocioException(
                    "Conta anonimizada nao pode ser editada: os dados pessoais foram apagados");
        }

        if (usuarioRepository.existsByEmailAndIdNot(dados.email(), usuarioId)) {
            throw new ConflitoException("Ja existe um usuario cadastrado com o e-mail: " + dados.email());
        }

        usuario.setNomeCompleto(dados.nomeCompleto());
        usuario.setEmail(dados.email());

        if (usuario.getPapel() == PapelUsuarioEnum.PROFESSOR) {
            exigirDadosDeProfessor(dados);

            if (usuarioRepository.existsByCrefAndIdNot(dados.cref(), usuarioId)) {
                throw new ConflitoException("Ja existe um professor cadastrado com este CREF");
            }

            usuario.setCref(dados.cref());
            usuario.setFormacao(dados.formacao());
            usuario.setCep(dados.cep());
            usuario.setLogradouro(dados.logradouro());
            usuario.setNumero(dados.numero());
            usuario.setComplemento(dados.complemento());
            usuario.setBairro(dados.bairro());
            usuario.setCidade(dados.cidade());
            usuario.setEstado(dados.estado());
        }

        UsuarioEntity salvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, salvo.getId());

        return usuarioMapper.toAdminDTO(salvo);
    }

    @Override
    @Transactional
    public UsuarioAdminResponseDTO alterarAtivo(UUID usuarioId, boolean ativo, UUID solicitanteId) {
        log.info("alterarAtivo: usuario={} ativo={}", usuarioId, ativo);

        if (!ativo && usuarioId.equals(solicitanteId)) {
            throw new RegraDeNegocioException("Voce nao pode inativar a propria conta");
        }

        UsuarioEntity usuario = buscarEntidade(usuarioId);

        if (usuario.getAnonimizadoEm() != null) {
            throw new RegraDeNegocioException(
                    "Conta anonimizada nao volta a ficar ativa: a anonimizacao e irreversivel");
        }

        usuario.setAtivo(ativo);

        UsuarioEntity salvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, salvo.getId());

        return usuarioMapper.toAdminDTO(salvo);
    }

    private UsuarioEntity buscarEntidade(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario nao encontrado: " + usuarioId));
    }

    private void exigirDadosDeProfessor(UsuarioAtualizacaoRequestDTO dados) {
        if (!preenchido(dados.cref()) || dados.formacao() == null) {
            throw new RegraDeNegocioException("CREF e formacao sao obrigatorios para professor");
        }

        if (!preenchido(dados.cep()) || !preenchido(dados.logradouro())
                || !preenchido(dados.numero()) || !preenchido(dados.bairro())
                || !preenchido(dados.cidade()) || !preenchido(dados.estado())) {
            throw new RegraDeNegocioException("O endereco completo e obrigatorio para professor");
        }
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private void registrarAceites(UsuarioEntity usuarioEntity){
        LocalDateTime agora = LocalDateTime.now(clock);

        usuarioEntity.setAceitouTermosUsoEm(agora);
        usuarioEntity.setVersaoTermosUso(
                documentoLegalService.versaoVigente(TipoDocumentoLegalEnum.TERMOS_USO));
        usuarioEntity.setAceitouPoliticaPrivacidadeEm(agora);
        usuarioEntity.setVersaoPoliticaPrivacidade(
                documentoLegalService.versaoVigente(TipoDocumentoLegalEnum.POLITICA_PRIVACIDADE)
        );
    }
}
