package forcamente.api.controller;

import forcamente.api.dto.QuizDetalheResponseDTO;
import forcamente.api.dto.QuizResumoResponseDTO;
import forcamente.api.service.IQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
