package forcamente.api.dto;

import java.time.LocalDate;

public record AtividadePorDiaResponseDTO(
        LocalDate dia,
        long total
) {
}
