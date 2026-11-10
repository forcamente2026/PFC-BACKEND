package forcamente.api.controller;

import forcamente.api.dto.EnderecoResponseDTO;
import forcamente.api.service.IEnderecoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enderecos")
@CrossOrigin(origins =  {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class EnderecoController {

    private final IEnderecoService enderecoService;

    @GetMapping("/{cep}")
    public ResponseEntity<EnderecoResponseDTO> buscarPorCep(@PathVariable("cep") String cep) {
        return ResponseEntity.ok(enderecoService.buscarPorCep(cep));
    }
}
