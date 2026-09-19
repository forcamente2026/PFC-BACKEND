package forcamente.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        String email,

        @NotBlank(message = "A senha e obrigatoria")
        String senha
) {
    public LoginRequestDTO {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
