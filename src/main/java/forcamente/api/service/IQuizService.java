package forcamente.api.service;

import forcamente.api.dto.QuizDetalheResponseDTO;
import forcamente.api.dto.QuizResumoResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IQuizService {

    List<QuizResumoResponseDTO> listarQuizzes();

    List<QuizResumoResponseDTO> listarPendentes();

    QuizDetalheResponseDTO buscarPorId(UUID quizId);
}
