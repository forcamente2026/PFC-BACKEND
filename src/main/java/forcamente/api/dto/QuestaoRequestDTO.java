package forcamente.api.dto;

import forcamente.api.validation.SemMarcacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

public record QuestaoRequestDTO(

        @NotBlank(message = "Informe o enunciado da pergunta")
        @Size(max = 250, message = "O enunciado deve ter no máximo 250 caracteres")
        @SemMarcacao
        String enunciado,

        @Size(max = 500, message = "A explicação deve ter no máximo 500 caracteres")
        @SemMarcacao
        String explicacao,

        @NotNull(message = "Inclua as alternativas")
        @Size(min = 2, max = 5, message = "Cada pergunta deve ter de 2 a 5 alternativas")
        @Valid
        List<@NotNull(message = "Informe a alternativa") AlternativaRequestDTO> alternativas
) {

    @AssertTrue(message = "Marque exatamente uma alternativa correta")
    public boolean isUmaAlternativaCorreta() {
        return alternativas == null
                || alternativas.stream()
                .filter(Objects::nonNull)
                .filter(AlternativaRequestDTO::correta)
                .count() == 1;
    }
}
