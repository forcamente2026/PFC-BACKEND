package forcamente.api.dto;

import forcamente.api.entity.enums.PapelUsuarioEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnonimizadorPendenteDTO(
        UUID id,
        String nomeCompleto,
        String email,
        PapelUsuarioEnum papel,
        LocalDateTime solicitadaEm
) {
}
