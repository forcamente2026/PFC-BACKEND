package forcamente.api.dto;

import forcamente.api.entity.enums.PapelUsuarioEnum;

import java.util.UUID;

public record LoginResponseDTO(UUID id, String token, String nomeCompleto, PapelUsuarioEnum papel) {
}
