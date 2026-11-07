package forcamente.api.dto;

import java.time.LocalDateTime;

public record TentativaHistoricoResponseDTO(
        String quiz,
        int acertos,
        int total,
        int percentual,
        LocalDateTime feitaEm
) {
}
