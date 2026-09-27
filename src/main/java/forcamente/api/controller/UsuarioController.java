package forcamente.api.controller;

import forcamente.api.dto.AnonimizadorPendenteDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.service.IUsuarioService;
import forcamente.api.service.impl.AnonimizacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final AnonimizacaoService anonimizacaoService;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criarUsuario(
            @Valid @RequestBody UsuarioRequestDTO usuarioRequestDTO) {

        var usuarioResponseDTO = usuarioService.criarUsuario(usuarioRequestDTO);

        var location = URI.create("/api/usuarios/" + usuarioResponseDTO.id());
        return ResponseEntity.created(location).body(usuarioResponseDTO);
    }

    @GetMapping("/formacoes")
    public ResponseEntity<List<OpcaoDTO>> listarFormacoes() {
        return ResponseEntity.ok(usuarioService.listarFormacoes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping("/me/anonimizacao")
    public ResponseEntity<Void> solicitarAnonimizacao(@AuthenticationPrincipal Jwt jwt) {
        anonimizacaoService.solicitar(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/anonimizacoes-pendentes")
    public ResponseEntity<List<AnonimizadorPendenteDTO>> listarAnonimizacoesPendentes() {
        return ResponseEntity.ok(anonimizacaoService.listarPendentes());
    }

    @PostMapping("/{id}/anonimizar")
    public ResponseEntity<Void> anonimizar(@PathVariable("id") UUID id) {
        anonimizacaoService.anonimizar(id);
        return ResponseEntity.noContent().build();
    }


}
