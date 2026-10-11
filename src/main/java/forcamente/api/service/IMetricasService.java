package forcamente.api.service;

import forcamente.api.dto.*;

import java.util.List;

public interface IMetricasService {

    VolumeTreinoResponseDTO calcularVolumeTreino(VolumeTreinoRequestDTO volumeTreinoRequestDTO);

    List<OpcaoDTO> listarNiveisAtividade();

    List<OpcaoDTO> listarSexosBiologicos();

    GastoEnergeticoResponseDTO calcularGastoEnergetico(GastoEnergeticoRequestDTO gastoEnergeticoRequestDTO);

    HidratacaoResponseDTO calcularHidratacao(HidratacaoRequestDTO hidratacaoRequestDTO);

}
