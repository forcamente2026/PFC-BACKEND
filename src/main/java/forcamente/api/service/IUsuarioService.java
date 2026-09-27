package forcamente.api.service;

import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.PaginaDTO;
import forcamente.api.dto.UsuarioAdminResponseDTO;
import forcamente.api.dto.UsuarioAtualizacaoRequestDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.enums.PapelUsuarioEnum;

import java.util.List;
import java.util.UUID;

public interface IUsuarioService {

    UsuarioResponseDTO criarUsuario(UsuarioRequestDTO usuarioRequestDTO);

    UsuarioResponseDTO buscarPorId(UUID usuarioId);

    List<OpcaoDTO> listarFormacoes();

    PaginaDTO<UsuarioAdminResponseDTO> listar(
            String busca, PapelUsuarioEnum papel, Boolean ativo, int pagina, int tamanho);

    UsuarioAdminResponseDTO atualizar(UUID usuarioId, UsuarioAtualizacaoRequestDTO dados);

    UsuarioAdminResponseDTO alterarAtivo(UUID usuarioId, boolean ativo, UUID solicitanteId);
}
