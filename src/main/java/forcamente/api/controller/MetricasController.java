package forcamente.api.controller;

import forcamente.api.dto.VolumeTreinoRequestDTO;
import forcamente.api.dto.VolumeTreinoResponseDTO;
import forcamente.api.service.IMetricasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import forcamente.api.dto.GastoEnergeticoRequestDTO;
import forcamente.api.dto.GastoEnergeticoResponseDTO;
import forcamente.api.dto.HidratacaoRequestDTO;
import forcamente.api.dto.HidratacaoResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import java.util.List;


@RestController
@RequestMapping("/api/metricas")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class MetricasController {

    private final IMetricasService metricasService;

    @PostMapping("/volume-treino")
    public ResponseEntity<VolumeTreinoResponseDTO> calcularVolumeTreino(
            @Valid @RequestBody VolumeTreinoRequestDTO volumeTreinoRequestDTO) {
        return
                ResponseEntity.ok(metricasService.calcularVolumeTreino(volumeTreinoRequestDTO));
    }

    @GetMapping("/niveis-atividade")
    public ResponseEntity<List<OpcaoDTO>> listarNiveisAtividade() {
        return ResponseEntity.ok(metricasService.listarNiveisAtividade());
    }

    @GetMapping("/sexos")
    public ResponseEntity<List<OpcaoDTO>> listarSexosBiologicos() {
        return ResponseEntity.ok(metricasService.listarSexosBiologicos());
    }

    @PostMapping("/gasto-energetico")
    public ResponseEntity<GastoEnergeticoResponseDTO> calcularGastoEnergetico(
            @Valid @RequestBody GastoEnergeticoRequestDTO gastoEnergeticoRequestDTO
    ) {
        return ResponseEntity.ok(metricasService.calcularGastoEnergetico(gastoEnergeticoRequestDTO));
    }

    @PostMapping("/hidratacao")
    public ResponseEntity<HidratacaoResponseDTO>calcularHidratacao(
            @Valid @RequestBody HidratacaoRequestDTO hidratacaoRequestDTO
    ) {
        return ResponseEntity.ok(metricasService.calcularHidratacao(hidratacaoRequestDTO));
    }
}

