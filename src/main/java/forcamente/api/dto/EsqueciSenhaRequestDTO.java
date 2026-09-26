package forcamente.api.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequestDTO(
        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        String email
) {
    public EsqueciSenhaRequestDTO {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
