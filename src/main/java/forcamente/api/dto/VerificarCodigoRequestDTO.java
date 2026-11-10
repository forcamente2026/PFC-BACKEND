package forcamente.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarCodigoRequestDTO(
        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        String email,

        @NotBlank(message = "O codigo e obrigatorio")
        @Pattern(regexp = "\\d{4}", message = "O codigo deve ter 4 digitos")
        String codigo
) {
    public VerificarCodigoRequestDTO {
        email = email == null ? null : email.trim().toLowerCase();
        codigo = codigo == null ? null : codigo.trim();
    }
}


