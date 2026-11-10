package forcamente.api.dto;

import forcamente.api.validation.SemMarcacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlternativaRequestDTO(

        @NotBlank(message = "Informe o texto da alternativa")
        @Size(max = 100, message = "A alternativa deve ter no máximo 100 caracteres")
        @SemMarcacao
        String texto,

        boolean correta
) {
}
