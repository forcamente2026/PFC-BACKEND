package forcamente.api.dto;

import forcamente.api.entity.enums.PapelUsuarioEnum;

public record LoginResponseDTO(String token, String nomeCompleto, PapelUsuarioEnum papel) {
}
