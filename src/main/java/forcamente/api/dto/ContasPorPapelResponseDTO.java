package forcamente.api.dto;

import forcamente.api.entity.enums.PapelUsuarioEnum;

public record ContasPorPapelResponseDTO(
        PapelUsuarioEnum papel,
        long total
) {
}
