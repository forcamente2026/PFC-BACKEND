package forcamente.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RespostaRequestDTO(

        @NotNull(message = "Informe a pergunta")
        UUID questaoId,

        @NotNull(message = "Informe a alternativa escolhida")
        UUID alternativaId
) {
}
