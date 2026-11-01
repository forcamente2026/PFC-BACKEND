package forcamente.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarEmailRequestDTO(

        @NotBlank(message = "Informe o e-mail novo")
        @Email(message = "Informe um e-mail valido")
        @Size(max = 255, message = "O e-mail deve ter no maximo 255 caracteres")
        String novoEmail,

        @NotBlank(message = "Informe a senha atual")
        String senhaAtual
) {
    public TrocarEmailRequestDTO {
        novoEmail = novoEmail == null ? null : novoEmail.trim().toLowerCase();
    }
}
