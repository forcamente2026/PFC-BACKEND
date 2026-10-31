package forcamente.api.service;

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
import forcamente.api.entity.enums.PapelUsuarioEnum;

import java.util.List;
import java.util.UUID;

public interface IUsuarioService {

    UsuarioResponseDTO criarUsuario(UsuarioRequestDTO usuarioRequestDTO);

    UsuarioResponseDTO buscarPorId(UUID usuarioId);

    UsuarioDetalheResponseDTO buscarDetalhePorId(UUID usuarioId);

    List<OpcaoDTO> listarFormacoes();

    PaginaDTO<UsuarioDetalheResponseDTO> listar(
            String busca, PapelUsuarioEnum papel, Boolean ativo, int pagina, int tamanho);

    UsuarioDetalheResponseDTO atualizar(UUID usuarioId, UsuarioAtualizacaoRequestDTO dados);

    List<VerificacaoPendenteDTO> listarPendentesDeVerificacao();

    UsuarioDetalheResponseDTO renovarCref(
            UUID usuarioId, RenovarCrefRequestDTO renovarCrefRequestDTO);

    UsuarioDetalheResponseDTO atualizarMinhaConta(
            UUID usuarioId, MinhaContaRequestDTO minhaContaRequestDTO);

    void trocarMinhaSenha(UUID usuarioId, TrocarSenhaRequestDTO trocarSenhaRequestDTO);

    UsuarioDetalheResponseDTO decidirVerificacaoDoCref(
            UUID usuarioId, AlterarStatusRequestDTO alterarStatusRequestDTO);

    UsuarioDetalheResponseDTO alterarAtivo(UUID usuarioId, boolean ativo, UUID solicitanteId);
}
