package forcamente.api.service.impl;

import forcamente.api.dto.OpcaoDTO;
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
        if (usuarioRequestDTO.cpf() != null && usuarioRepository.existsByCpf(usuarioRequestDTO.cpf())) {
            throw new ConflitoException("Ja existe um usuario cadastrado com este CPF");
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
