package forcamente.api.controller;

import forcamente.api.config.ModoApresentacao;
import forcamente.api.dto.ModoApresentacaoRequestDTO;
import forcamente.api.dto.ModoApresentacaoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracao")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ModoApresentacao modoApresentacao;

    @GetMapping("/modo-apresentacao")
    public ResponseEntity<ModoApresentacaoResponseDTO> consultar() {
        return ResponseEntity.ok(resposta());
    }

    @PatchMapping("/modo-apresentacao")
    public ResponseEntity<ModoApresentacaoResponseDTO> definir(
            @Valid @RequestBody ModoApresentacaoRequestDTO modoApresentacaoRequestDTO) {

        modoApresentacao.definir(modoApresentacaoRequestDTO.ativo());

        return ResponseEntity.ok(resposta());
    }

    private ModoApresentacaoResponseDTO resposta() {
        return new ModoApresentacaoResponseDTO(
                modoApresentacao.isAtivo(),
                modoApresentacao.isAtivo() ? modoApresentacao.getCodigoFixo() : null);
    }
}
