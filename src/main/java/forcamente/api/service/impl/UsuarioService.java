package forcamente.api.service.impl;

import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.CategoriaProfissionalEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.mapper.UsuarioMapper;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IUsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (usuarioRepository.existsByCpf(usuarioRequestDTO.cpf())) {
            throw new ConflitoException("Ja existe um usuario cadastrado com este CPF");
        }
        if (usuarioRequestDTO.cref() != null && usuarioRepository.existsByCref(usuarioRequestDTO.cref())) {
            throw new ConflitoException("Ja existe um professor cadastrado com este CREF");
        }

        UsuarioEntity usuarioEntity = usuarioMapper.toEntity(usuarioRequestDTO);

        usuarioEntity.setSenhaHash(passwordEncoder.encode(usuarioRequestDTO.senha()));

        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuarioEntity);

        return usuarioMapper.toDTO(usuarioSalvo);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(UUID usuarioId) {
        log.info("buscarPorId: {}", usuarioId);

        UsuarioEntity usuarioEntity = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario nao encontrado: " + usuarioId));

        return usuarioMapper.toDTO(usuarioEntity);
    }

    @Override
    public List<OpcaoDTO> listarCategoriasProfissionais() {
        log.info("listarCategoriasProfissionais");
        return Arrays.stream(CategoriaProfissionalEnum.values()).map(categoria -> new OpcaoDTO(categoria.name(), categoria.getDescricao())).toList();
    }
}
