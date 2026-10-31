package forcamente.api.dto;

import forcamente.api.validation.SemMarcacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MinhaContaRequestDTO(

        @NotBlank(message = "O nome completo e obrigatorio")
        @SemMarcacao
        String nomeCompleto,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 dígitos, apenas números")
        String cep,

        @SemMarcacao
        String logradouro,

        @SemMarcacao
        String numero,

        @SemMarcacao
        String complemento,

        @SemMarcacao
        String bairro,

        @SemMarcacao
        String cidade,

        @Size(max = 2, message = "O estado deve ser a sigla com 2 letras")
        @SemMarcacao
        String estado
) {
}
