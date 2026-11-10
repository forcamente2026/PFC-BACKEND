package forcamente.api.controller;

import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.PaginaDTO;
import forcamente.api.dto.RegistroAuditoriaResponseDTO;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.service.IAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class AuditoriaController {

    private static final String MARCA_UTF8 = "\uFEFF";

    private final IAuditoriaService auditoriaService;

    private final Clock clock;

    @GetMapping
    public ResponseEntity<PaginaDTO<RegistroAuditoriaResponseDTO>> consultar(
            @RequestParam(name = "de", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,

            @RequestParam(name = "ate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,

            @RequestParam(name = "acao", required = false) AcaoAuditoriaEnum acao,

            @RequestParam(name = "pagina", defaultValue = "0") int pagina,

            @RequestParam(name = "tamanho", defaultValue = "50") int tamanho) {

        auditoriaService.registrar(AcaoAuditoriaEnum.LIDO, RecursoAuditoriaEnum.AUDITORIA, null);

        return ResponseEntity.ok(auditoriaService.consultar(de, ate, acao, pagina, tamanho));
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(name = "de", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,

            @RequestParam(name = "ate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,

            @RequestParam(name = "acao", required = false) AcaoAuditoriaEnum acao) {

        auditoriaService.registrar(AcaoAuditoriaEnum.LIDO, RecursoAuditoriaEnum.AUDITORIA, null);

        String csv = MARCA_UTF8 + auditoriaService.exportarCsv(de, ate, acao);
        String arquivo = "auditoria-" + LocalDate.now(clock) + ".csv";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo + "\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/acoes")
    public ResponseEntity<List<OpcaoDTO>> listarAcoes() {
        return ResponseEntity.ok(auditoriaService.listarAcoes());
    }

    @GetMapping("/recursos")
    public ResponseEntity<List<OpcaoDTO>> listarRecursos() {
        return ResponseEntity.ok(auditoriaService.listarRecursos());
    }
}
