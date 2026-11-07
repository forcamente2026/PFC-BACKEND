package forcamente.api.controller;

import forcamente.api.dto.AtividadePorDiaResponseDTO;
import forcamente.api.dto.ContasPorPapelResponseDTO;
import forcamente.api.service.IEstatisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estatisticas")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class EstatisticasController {

    private final IEstatisticasService estatisticasService;

    @GetMapping("/contas-por-papel")
    public ResponseEntity<List<ContasPorPapelResponseDTO>> contarContasPorPapel() {
        return ResponseEntity.ok(estatisticasService.contarContasPorPapel());
    }

    @GetMapping("/atividade-por-dia")
    public ResponseEntity<List<AtividadePorDiaResponseDTO>> contarAtividadePorDia(
            @RequestParam(name = "dias", defaultValue = "7") int dias) {

        return ResponseEntity.ok(estatisticasService.contarAtividadePorDia(dias));
    }
}
