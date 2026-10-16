package forcamente.api.dto;

import forcamente.api.entity.enums.CategoriaArtigoEnum;
import jakarta.validation.constraints.*;

public record ArtigoRequestDTO(
        @NotBlank(message = "Informe o título")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        String titulo,

        @NotNull(message = "Informe a categoria")
        CategoriaArtigoEnum categoria,

        @NotBlank(message = "Informe o resumo")
        @Size(max = 2000, message = "O resumo deve ter no máximo 2000 caracteres")
        String resumo,

        @NotBlank(message = "Informe a fonte")
        @Size(max = 500, message = "A fonte deve ter no máximo 500 caracteres")
        @Pattern(
                regexp = "^\\s*https://[^\\s@]+(/\\S*)?\\s*$",
                message = "A fonte deve ser um endereço https, sem usuário e senha no endereço")
        String fonte,

        @Size(max = 255, message = "A autoria deve ter no máximo 255 caracteres")
        String autoria,

        @Size(max = 255, message = "O tipo de estudo deve ter no máximo 255 caracteres")
        String tipoEstudo,

        @Min(value = 1900, message = "O ano deve ser a partir de 1900")
        Integer ano,

        @Min(value = 1, message = "Os minutos de leitura devem ser pelo menos 1")
        @Max(value = 600, message = "Os minutos de leitura devem ser no máximo 600")
        Integer minutosLeitura
) {
}
