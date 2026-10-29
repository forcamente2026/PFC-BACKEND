package forcamente.api.controller;

import forcamente.api.dto.QuizDetalheResponseDTO;
import forcamente.api.dto.QuizEdicaoResponseDTO;
import forcamente.api.dto.QuizRequestDTO;
import forcamente.api.dto.QuizResumoResponseDTO;
import forcamente.api.service.IQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class QuizController {

    private final IQuizService quizService;

    @GetMapping
    public ResponseEntity<List<QuizResumoResponseDTO>> listarQuizzes() {
        return ResponseEntity.ok(quizService.listarQuizzes());
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<QuizResumoResponseDTO>> listarPendentes() {
        return ResponseEntity.ok(quizService.listarPendentes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDetalheResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.buscarPorId(id));
    }

    @GetMapping("/{id}/edicao")
    public ResponseEntity<QuizEdicaoResponseDTO> buscarParaEdicao(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.buscarParaEdicao(id));
    }

    @PostMapping
    public ResponseEntity<QuizDetalheResponseDTO> criarQuiz(
            @Valid @RequestBody QuizRequestDTO quizRequestDTO
    ) {
        var criado = quizService.criarQuiz(quizRequestDTO);

        return ResponseEntity.created(URI.create("/api/quizzes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizDetalheResponseDTO> atualizarQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody QuizRequestDTO quizRequestDTO
    ) {
        return ResponseEntity.ok(quizService.atualizarQuiz(id, quizRequestDTO));
    }
}
