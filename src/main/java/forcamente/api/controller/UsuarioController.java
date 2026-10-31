package forcamente.api.controller;

import forcamente.api.dto.AlterarAtivoRequestDTO;
import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.AnonimizadorPendenteDTO;
import forcamente.api.dto.MinhaContaRequestDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.PaginaDTO;
import forcamente.api.dto.RenovarCrefRequestDTO;
import forcamente.api.dto.TrocarSenhaRequestDTO;
import forcamente.api.dto.UsuarioDetalheResponseDTO;
import forcamente.api.dto.VerificacaoPendenteDTO;
import forcamente.api.dto.UsuarioAtualizacaoRequestDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.service.IUsuarioService;
import forcamente.api.service.impl.AnonimizacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/pendentes-verificacao")
    public ResponseEntity<List<VerificacaoPendenteDTO>> listarPendentesDeVerificacao() {
        return ResponseEntity.ok(usuarioService.listarPendentesDeVerificacao());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        boolean administrador =
                PapelUsuarioEnum.ADMINISTRADOR.name().equals(jwt.getClaimAsString("papel"));
        boolean ehAPropriaConta = id.toString().equals(jwt.getSubject());

        if (!administrador && !ehAPropriaConta) {
            throw new AccessDeniedException("Acesso negado");
        }

        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<UsuarioDetalheResponseDTO>> listar(
            @RequestParam(name = "busca", required = false) String busca,

            @RequestParam(name = "papel", required = false) PapelUsuarioEnum papel,

            @RequestParam(name = "ativo", required = false) Boolean ativo,

            @RequestParam(name = "pagina", defaultValue = "0") int pagina,

            @RequestParam(name = "tamanho", defaultValue = "20") int tamanho) {

        return ResponseEntity.ok(usuarioService.listar(busca, papel, ativo, pagina, tamanho));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDetalheResponseDTO> atualizar(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UsuarioAtualizacaoRequestDTO dados) {

        return ResponseEntity.ok(usuarioService.atualizar(id, dados));
    }

    @PatchMapping("/{id}/ativo")
    public ResponseEntity<UsuarioDetalheResponseDTO> alterarAtivo(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AlterarAtivoRequestDTO corpo,
            @AuthenticationPrincipal Jwt jwt) {

        UUID solicitanteId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(usuarioService.alterarAtivo(id, corpo.ativo(), solicitanteId));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDetalheResponseDTO> minhaConta(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                usuarioService.buscarDetalhePorId(UUID.fromString(jwt.getSubject())));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioDetalheResponseDTO> atualizarMinhaConta(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MinhaContaRequestDTO minhaContaRequestDTO) {

        return ResponseEntity.ok(usuarioService.atualizarMinhaConta(
                UUID.fromString(jwt.getSubject()), minhaContaRequestDTO));
    }

    @PatchMapping("/me/senha")
    public ResponseEntity<Void> trocarMinhaSenha(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TrocarSenhaRequestDTO trocarSenhaRequestDTO) {

        usuarioService.trocarMinhaSenha(UUID.fromString(jwt.getSubject()), trocarSenhaRequestDTO);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/cref")
    public ResponseEntity<UsuarioDetalheResponseDTO> renovarCref(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RenovarCrefRequestDTO renovarCrefRequestDTO) {

        return ResponseEntity.ok(usuarioService.renovarCref(
                UUID.fromString(jwt.getSubject()), renovarCrefRequestDTO));
    }

    @DeleteMapping("/me/anonimizacao")
    public ResponseEntity<Void> cancelarAnonimizacao(@AuthenticationPrincipal Jwt jwt) {
        anonimizacaoService.cancelarSolicitacao(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
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

    @PatchMapping("/{id}/verificacao")
    public ResponseEntity<UsuarioDetalheResponseDTO> decidirVerificacaoDoCref(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AlterarStatusRequestDTO alterarStatusRequestDTO) {

        return ResponseEntity.ok(
                usuarioService.decidirVerificacaoDoCref(id, alterarStatusRequestDTO));
    }
}
