package forcamente.api.controller;


import forcamente.api.dto.*;
import forcamente.api.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor


public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginPendenteResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return ResponseEntity.ok(authService.login(loginRequestDTO));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(
            @Valid @RequestBody EsqueciSenhaRequestDTO esqueciSenhaRequestDTO) {
        authService.esqueciSenha(esqueciSenhaRequestDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequestDTO redefinirSenhaRequestDTO) {
        authService.redefinirSenha(redefinirSenhaRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login/verificar")
    public ResponseEntity<LoginResponseDTO> verificarCodigo(
            @Valid @RequestBody VerificarCodigoRequestDTO verificarCodigoRequestDTO) {
        return ResponseEntity.ok(authService.verificarCodigo(verificarCodigoRequestDTO));
    }

}
