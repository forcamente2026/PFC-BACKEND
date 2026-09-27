package forcamente.api.service;

import forcamente.api.dto.AnonimizadorPendenteDTO;

import java.util.List;
import java.util.UUID;

public interface IAnonimizacaoService {


    void solicitar(UUID usuarioId);

    List<AnonimizadorPendenteDTO> listarPendentes();

    void anonimizar(UUID usuarioId);
}

