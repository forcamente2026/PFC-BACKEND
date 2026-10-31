package forcamente.api.service.impl;

import forcamente.api.dto.MinhaContaRequestDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.PaginaDTO;
import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.RenovarCrefRequestDTO;
import forcamente.api.dto.TrocarSenhaRequestDTO;
import forcamente.api.dto.UsuarioDetalheResponseDTO;
import forcamente.api.dto.VerificacaoPendenteDTO;
import forcamente.api.dto.UsuarioAtualizacaoRequestDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.LimiteDeTentativasException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService implements IUsuarioService {

    private static final int CREF_REENVIOS_POR_DIA = 3;

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

        if (usuarioEntity.getPapel() == PapelUsuarioEnum.PROFESSOR) {
            usuarioEntity.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);
        }

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
    @Transactional(readOnly = true)
    public UsuarioDetalheResponseDTO buscarDetalhePorId(UUID usuarioId) {
        log.info("buscarDetalhePorId: {}", usuarioId);

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario nao encontrado: " + usuarioId));

        auditoriaService.registrar(AcaoAuditoriaEnum.LIDO, RecursoAuditoriaEnum.USUARIO, usuarioId);

        return usuarioMapper.toDetalheDTO(usuario);
    }

    @Override
    public List<OpcaoDTO> listarFormacoes() {
        log.info("listarFormacoes");
        return Arrays.stream(FormacaoEnum.values()).map(formacao -> new OpcaoDTO(formacao.name(), formacao.getDescricao() )).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<UsuarioDetalheResponseDTO> listar(
            String busca, PapelUsuarioEnum papel, Boolean ativo, int pagina, int tamanho) {

        log.info("listar usuarios: busca={} papel={} ativo={}", busca, papel, ativo);

        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();

        Page<UsuarioEntity> resultado = usuarioRepository.buscarComFiltros(
                papel, ativo, termo, PageRequest.of(pagina, tamanho));

        auditoriaService.registrar(AcaoAuditoriaEnum.LIDO, RecursoAuditoriaEnum.USUARIO, null);

        return new PaginaDTO<>(
                resultado.getContent().stream().map(usuarioMapper::toDetalheDTO).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Override
    @Transactional
    public UsuarioDetalheResponseDTO atualizar(UUID usuarioId, UsuarioAtualizacaoRequestDTO dados) {
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

            boolean crefOuFormacaoMudou =
                    !Objects.equals(usuario.getCref(), dados.cref())
                            || usuario.getFormacao() != dados.formacao();

            if (crefOuFormacaoMudou
                    && usuario.getSituacaoCref() == StatusAprovacaoEnum.REJEITADO) {

                usuario.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);
                usuario.setMotivoRejeicaoCref(null);
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

        return usuarioMapper.toDetalheDTO(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificacaoPendenteDTO> listarPendentesDeVerificacao() {
        log.info("listarPendentesDeVerificacao");

        return usuarioRepository
                .findByPapelAndSituacaoCrefOrderByCriadoEm(
                        PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.PENDENTE)
                .stream()
                .map(usuario -> new VerificacaoPendenteDTO(
                        usuario.getId(),
                        usuario.getNomeCompleto(),
                        usuario.getEmail(),
                        usuario.getCref(),
                        usuario.getFormacao(),
                        usuario.getCriadoEm()))
                .toList();
    }

    @Override
    @Transactional
    public UsuarioDetalheResponseDTO renovarCref(
            UUID usuarioId, RenovarCrefRequestDTO renovarCrefRequestDTO) {

        log.info("renovarCref: {}", usuarioId);

        UsuarioEntity usuario = buscarEntidade(usuarioId);

        if (usuario.getPapel() != PapelUsuarioEnum.PROFESSOR) {
            throw new RegraDeNegocioException(
                    "Somente a conta de professor tem CREF para renovar");
        }

        if (usuario.getSituacaoCref() != StatusAprovacaoEnum.APROVADO
                && usuario.getSituacaoCref() != StatusAprovacaoEnum.REJEITADO) {

            throw new RegraDeNegocioException(
                    "A correcao do CREF vale com a verificacao aprovada ou rejeitada. "
                            + "Enquanto ela estiver pendente, a conferencia ainda esta "
                            + "com o administrador");
        }

        if (usuarioRepository.existsByCrefAndIdNot(renovarCrefRequestDTO.cref(), usuarioId)) {
            throw new ConflitoException("Ja existe um professor cadastrado com este CREF");
        }

        contarReenvioDoDia(usuario);

        usuario.setCref(renovarCrefRequestDTO.cref());
        usuario.setFormacao(renovarCrefRequestDTO.formacao());
        usuario.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);
        usuario.setMotivoRejeicaoCref(null);

        UsuarioEntity salvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, salvo.getId());

        return usuarioMapper.toDetalheDTO(salvo);
    }

    @Override
    @Transactional
    public UsuarioDetalheResponseDTO atualizarMinhaConta(
            UUID usuarioId, MinhaContaRequestDTO minhaContaRequestDTO) {

        log.info("atualizarMinhaConta: {}", usuarioId);

        UsuarioEntity usuario = buscarEntidade(usuarioId);

        boolean professor = usuario.getPapel() == PapelUsuarioEnum.PROFESSOR;

        if (professor) {
            exigirEnderecoCompleto(minhaContaRequestDTO);
        } else {
            exigirSemEndereco(minhaContaRequestDTO);
        }

        usuario.setNomeCompleto(minhaContaRequestDTO.nomeCompleto().trim());

        if (professor) {
            usuario.setCep(minhaContaRequestDTO.cep());
            usuario.setLogradouro(minhaContaRequestDTO.logradouro().trim());
            usuario.setNumero(minhaContaRequestDTO.numero().trim());
            usuario.setComplemento(aparado(minhaContaRequestDTO.complemento()));
            usuario.setBairro(minhaContaRequestDTO.bairro().trim());
            usuario.setCidade(minhaContaRequestDTO.cidade().trim());
            usuario.setEstado(minhaContaRequestDTO.estado().trim().toUpperCase());
        }

        UsuarioEntity salvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, salvo.getId());

        return usuarioMapper.toDetalheDTO(salvo);
    }

    @Override
    @Transactional
    public void trocarMinhaSenha(UUID usuarioId, TrocarSenhaRequestDTO trocarSenhaRequestDTO) {
        log.info("trocarMinhaSenha: {}", usuarioId);

        UsuarioEntity usuario = buscarEntidade(usuarioId);

        if (!passwordEncoder.matches(trocarSenhaRequestDTO.senhaAtual(), usuario.getSenhaHash())) {
            auditoriaService.registrar(
                    AcaoAuditoriaEnum.SENHA_FALHOU, RecursoAuditoriaEnum.USUARIO, usuarioId);

            throw new RegraDeNegocioException("A senha atual nao confere");
        }

        usuario.setSenhaHash(passwordEncoder.encode(trocarSenhaRequestDTO.novaSenha()));

        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, usuarioId);
    }

    private void exigirEnderecoCompleto(MinhaContaRequestDTO minhaConta) {
        boolean faltaAlgum = vazio(minhaConta.cep())
                || vazio(minhaConta.logradouro())
                || vazio(minhaConta.numero())
                || vazio(minhaConta.bairro())
                || vazio(minhaConta.cidade())
                || vazio(minhaConta.estado());

        if (faltaAlgum) {
            throw new RegraDeNegocioException(
                    "O endereco profissional e obrigatorio para o professor, "
                            + "menos o complemento");
        }
    }

    private void exigirSemEndereco(MinhaContaRequestDTO minhaConta) {
        boolean algumPreenchido = !vazio(minhaConta.cep())
                || !vazio(minhaConta.logradouro())
                || !vazio(minhaConta.numero())
                || !vazio(minhaConta.complemento())
                || !vazio(minhaConta.bairro())
                || !vazio(minhaConta.cidade())
                || !vazio(minhaConta.estado());

        if (algumPreenchido) {
            throw new RegraDeNegocioException(
                    "O endereco e guardado somente para a conta de professor");
        }
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String aparado(String valor) {
        return vazio(valor) ? null : valor.trim();
    }

    private void contarReenvioDoDia(UsuarioEntity usuario) {
        LocalDate hoje = LocalDate.now(clock);

        if (!hoje.equals(usuario.getCrefReenvioDia())) {
            usuario.setCrefReenvioDia(hoje);
            usuario.setCrefReenvios(0);
        }

        if (usuario.getCrefReenvios() >= CREF_REENVIOS_POR_DIA) {
            throw new LimiteDeTentativasException(
                    "Limite de " + CREF_REENVIOS_POR_DIA
                            + " renovacoes de CREF por dia atingido. Tente novamente amanha");
        }

        usuario.setCrefReenvios(usuario.getCrefReenvios() + 1);
    }

    @Override
    @Transactional
    public UsuarioDetalheResponseDTO decidirVerificacaoDoCref(
            UUID usuarioId, AlterarStatusRequestDTO alterarStatusRequestDTO) {

        log.info("decidirVerificacaoDoCref: {} para {}",
                usuarioId, alterarStatusRequestDTO.status());

        UsuarioEntity usuario = buscarEntidade(usuarioId);

        if (usuario.getPapel() != PapelUsuarioEnum.PROFESSOR) {
            throw new RegraDeNegocioException(
                    "A verificacao de CREF se aplica somente a conta de professor");
        }

        if (usuario.getSituacaoCref() != StatusAprovacaoEnum.PENDENTE) {
            throw new ConflitoException(
                    "Esta verificacao nao esta pendente e por isso nao pode ser decidida");
        }

        boolean rejeitado =
                alterarStatusRequestDTO.status() == StatusAprovacaoEnum.REJEITADO;

        usuario.setSituacaoCref(alterarStatusRequestDTO.status());
        usuario.setMotivoRejeicaoCref(
                rejeitado ? alterarStatusRequestDTO.motivo().trim() : null);

        UsuarioEntity salvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(
                rejeitado ? AcaoAuditoriaEnum.REJEITADO : AcaoAuditoriaEnum.APROVADO,
                RecursoAuditoriaEnum.USUARIO, salvo.getId());

        return usuarioMapper.toDetalheDTO(salvo);
    }

    @Override
    @Transactional
    public UsuarioDetalheResponseDTO alterarAtivo(UUID usuarioId, boolean ativo, UUID solicitanteId) {
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

        return usuarioMapper.toDetalheDTO(salvo);
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
