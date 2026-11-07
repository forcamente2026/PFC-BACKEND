package forcamente.api.service;

import forcamente.api.dto.AtividadePorDiaResponseDTO;
import forcamente.api.dto.ContasPorPapelResponseDTO;

import java.util.List;

public interface IEstatisticasService {

    List<ContasPorPapelResponseDTO> contarContasPorPapel();

    List<AtividadePorDiaResponseDTO> contarAtividadePorDia(int dias);
}
