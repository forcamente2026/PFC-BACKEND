package forcamente.api.service;

import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.QuizDetalheResponseDTO;
import forcamente.api.dto.QuizEdicaoResponseDTO;
import forcamente.api.dto.QuizRequestDTO;
import forcamente.api.dto.QuizResumoResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IQuizService {

    List<QuizResumoResponseDTO> listarQuizzes();

    List<QuizResumoResponseDTO> listarPendentes();

    QuizDetalheResponseDTO buscarPorId(UUID quizId);

    QuizEdicaoResponseDTO buscarParaEdicao(UUID quizId);

    QuizDetalheResponseDTO criarQuiz(QuizRequestDTO quizRequestDTO);

    QuizDetalheResponseDTO atualizarQuiz(UUID quizId, QuizRequestDTO quizRequestDTO);

    void excluirQuiz(UUID quizId);

    QuizDetalheResponseDTO aprovarComoProfessor(UUID quizId);

    QuizDetalheResponseDTO decidirSituacao(UUID quizId, AlterarStatusRequestDTO alterarStatusRequestDTO);
}
