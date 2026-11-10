package forcamente.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TentativaRequestDTO(

        @NotEmpty(message = "Responda todas as perguntas")
        @Valid
        List<@NotNull(message = "Informe a resposta") RespostaRequestDTO> respostas
) {
}
