package forcamente.api.service;

import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IUsuarioService {

    UsuarioResponseDTO criarUsuario(UsuarioRequestDTO usuarioRequestDTO);

    UsuarioResponseDTO buscarPorId(UUID usuarioId);

    List<OpcaoDTO> listarCategoriasProfissionais();
}
