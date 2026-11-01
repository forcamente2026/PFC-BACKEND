package forcamente.api.dto;

import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.validation.SemMarcacao;
import jakarta.validation.constraints.*;

public record ArtigoRequestDTO(
        @NotBlank(message = "Informe o título")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        @SemMarcacao
        String titulo,

        @NotNull(message = "Informe a categoria")
        CategoriaArtigoEnum categoria,

        @NotBlank(message = "Informe o resumo")
        @Size(min = 250, max = 1000,
                message = "O resumo deve ter de 250 a 1000 caracteres")
        @SemMarcacao
        String resumo,

        @NotBlank(message = "Informe a fonte")
        @Size(max = 500, message = "A fonte deve ter no máximo 500 caracteres")
        @Pattern(
                regexp = "^\\s*https://[^\\s@]+(/\\S*)?\\s*$",
                message = "A fonte deve ser um endereço https, sem usuário e senha no endereço")
        String fonte,

        @Size(max = 255, message = "A autoria deve ter no máximo 255 caracteres")
        @SemMarcacao
        String autoria,

        @Size(max = 255, message = "O tipo de estudo deve ter no máximo 255 caracteres")
        @SemMarcacao
        String tipoEstudo,

        @Min(value = 1900, message = "O ano deve ser a partir de 1900")
        Integer ano
) {
}
