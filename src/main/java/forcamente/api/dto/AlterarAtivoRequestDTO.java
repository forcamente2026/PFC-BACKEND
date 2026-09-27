package forcamente.api.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarAtivoRequestDTO(
        @NotNull(message = "Informe se a conta fica ativa ou inativa")
        Boolean ativo
) {
}
