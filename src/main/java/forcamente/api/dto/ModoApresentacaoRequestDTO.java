package forcamente.api.dto;

import jakarta.validation.constraints.NotNull;

public record ModoApresentacaoRequestDTO(
        @NotNull(message = "Informe se o modo de apresentacao fica ativo")
        Boolean ativo
) {
}
