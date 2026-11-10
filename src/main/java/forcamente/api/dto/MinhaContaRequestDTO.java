package forcamente.api.dto;

import forcamente.api.validation.SemMarcacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MinhaContaRequestDTO(

        @NotBlank(message = "O nome completo e obrigatorio")
        @Size(max = 150, message = "O nome completo deve ter no máximo 150 caracteres")
        @SemMarcacao
        String nomeCompleto,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 dígitos, apenas números")
        String cep,

        @SemMarcacao
        @Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres")
        String logradouro,

        @SemMarcacao
        @Size(max = 10, message = "O número deve ter no máximo 10 caracteres")
        String numero,

        @SemMarcacao
        @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres")
        String complemento,

        @SemMarcacao
        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
        String bairro,

        @SemMarcacao
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String cidade,

        @Size(max = 2, message = "O estado deve ser a sigla com 2 letras")
        @SemMarcacao
        String estado
) {
}
