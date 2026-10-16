package forcamente.api.controller;

import forcamente.api.dto.AlterarStatusArtigoRequestDTO;
import forcamente.api.dto.ArtigoRequestDTO;
import forcamente.api.dto.ArtigoResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.service.IArtigoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/artigos")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class ArtigoController {

    private final IArtigoService artigoService;

    @GetMapping
    public ResponseEntity<List<ArtigoResponseDTO>> listarArtigos(
            @RequestParam(name = "status", required = false)StatusAprovacaoEnum status
            ) {
        return ResponseEntity.ok(artigoService.listarArtigos(status));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<OpcaoDTO>> listarCategorias() {
        return ResponseEntity.ok(artigoService.listarCategorias());
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<ArtigoResponseDTO>> listarPendentes() {
        return ResponseEntity.ok(artigoService.listarPendentes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtigoResponseDTO> buscarPorId(@PathVariable("id")
                                                         UUID id) {
        return ResponseEntity.ok(artigoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ArtigoResponseDTO> criarArtigo(
            @Valid @RequestBody ArtigoRequestDTO artigoRequestDTO
            ) {
        var artigo = artigoService.criarArtigo(artigoRequestDTO);

        var location = URI.create("/api/artigos/" + artigo.id());
        return ResponseEntity.created(location).body(artigo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArtigoResponseDTO> atualizarArtigo(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ArtigoRequestDTO artigoRequestDTO) {
        return ResponseEntity.ok(artigoService.atualizarArtigo(id, artigoRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirArtigo(@PathVariable("id") UUID id){
        artigoService.excluirArtigo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aprovacoes")
    public ResponseEntity<ArtigoResponseDTO> aprovarComoProfessor(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(artigoService.aprovarComoProfessor(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ArtigoResponseDTO> decidirSituacao(@PathVariable("id") UUID id, @Valid @RequestBody AlterarStatusArtigoRequestDTO alterarStatusArtigoRequestDTO) {
        return ResponseEntity.ok(artigoService.decidirSituacao(id, alterarStatusArtigoRequestDTO));
    }

}
