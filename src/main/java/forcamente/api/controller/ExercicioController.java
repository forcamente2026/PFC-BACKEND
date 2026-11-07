package forcamente.api.controller;

import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.ExercicioRequestDTO;
import forcamente.api.dto.ExercicioResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.service.IExercicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exercicios")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class ExercicioController {

    private final IExercicioService exercicioService;

    @PostMapping
    public ResponseEntity<ExercicioResponseDTO> criarExercicio(
            @Valid @RequestBody ExercicioRequestDTO exercicioRequestDTO) {

        var exercicioResponseDTO = exercicioService.criarExercicio(exercicioRequestDTO);

        var location = URI.create("/api/exercicios/" + exercicioResponseDTO.id());
        return ResponseEntity.created(location).body(exercicioResponseDTO);
    }

    @GetMapping
    public ResponseEntity<List<ExercicioResponseDTO>> listarExercicios(
            @RequestParam(name = "grupoMuscular", required = false) GrupoMuscularEnum grupoMuscular) {

        var exercicios = (grupoMuscular == null)
                ? exercicioService.listarExercicios()
                : exercicioService.listarPorGrupoMuscular(grupoMuscular);

        return ResponseEntity.ok(exercicios);
    }

    @PostMapping("/{id}/aprovacoes")
    public ResponseEntity<ExercicioResponseDTO> aprovarComoProfessor(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(exercicioService.aprovarComoProfessor(id));
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<ExercicioResponseDTO>> listarPendentes() {
        return ResponseEntity.ok(exercicioService.listarPendentes());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ExercicioResponseDTO> decidirSituacao(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AlterarStatusRequestDTO alterarStatusRequestDTO) {

        return ResponseEntity.ok(exercicioService.decidirSituacao(id, alterarStatusRequestDTO));
    }

    @GetMapping("/grupos-musculares")
    public ResponseEntity<List<OpcaoDTO>> listarGruposMusculares() {
        return ResponseEntity.ok(exercicioService.listarGruposMusculares());
    }

    @GetMapping("/niveis")
    public ResponseEntity<List<OpcaoDTO>> listarNiveis() {
        return ResponseEntity.ok(exercicioService.listarNiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExercicioResponseDTO> buscarPorId(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(exercicioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExercicioResponseDTO> atualizarExercicio(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ExercicioRequestDTO exercicioRequestDTO){

        return ResponseEntity.ok(exercicioService.atualizarExercicio(id, exercicioRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirExercicio(@PathVariable("id") UUID id){
        exercicioService.excluirExercicio(id);
        return ResponseEntity.noContent().build();
    }
}
