package forcamente.api.service;

import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.ArtigoRequestDTO;
import forcamente.api.dto.ArtigoResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.entity.enums.StatusAprovacaoEnum;

import java.util.List;
import java.util.UUID;

public interface IArtigoService {

    void excluirArtigo(UUID artigoId);

    ArtigoResponseDTO aprovarComoProfessor(UUID artigoId);

    List<ArtigoResponseDTO> listarPendentes();

    ArtigoResponseDTO decidirSituacao(UUID artigoId, AlterarStatusRequestDTO alterarStatusArtigoRequestDTO);

    List<ArtigoResponseDTO> listarArtigos(StatusAprovacaoEnum status);

    ArtigoResponseDTO buscarPorId(UUID artigoId);

    ArtigoResponseDTO criarArtigo(ArtigoRequestDTO artigoRequestDTO);

    ArtigoResponseDTO atualizarArtigo(UUID artigoId, ArtigoRequestDTO artigoRequestDTO);

    List<OpcaoDTO> listarCategorias();


}
