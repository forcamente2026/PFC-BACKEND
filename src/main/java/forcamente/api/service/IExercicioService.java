package forcamente.api.service;

import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.ExercicioRequestDTO;
import forcamente.api.dto.ExercicioResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import java.util.List;
import java.util.UUID;

public interface IExercicioService {

    ExercicioResponseDTO criarExercicio(ExercicioRequestDTO exercicioRequestDTO);

    List<ExercicioResponseDTO> listarExercicios();

    List<ExercicioResponseDTO> listarPorGrupoMuscular(GrupoMuscularEnum grupoMuscular);

    ExercicioResponseDTO buscarPorId(UUID exercicioId);

    ExercicioResponseDTO atualizarExercicio(UUID exercicioId, ExercicioRequestDTO exercicioRequestDTO);

    void excluirExercicio(UUID exercicioId);

    ExercicioResponseDTO aprovarComoProfessor(UUID exercicioId);

    List<ExercicioResponseDTO> listarPendentes();

    ExercicioResponseDTO decidirSituacao(UUID exercicioId, AlterarStatusRequestDTO alterarStatusRequestDTO);

    List<OpcaoDTO> listarGruposMusculares();

    List<OpcaoDTO> listarNiveis();
}
