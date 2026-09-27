package forcamente.api.dto;


import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String nomeCompleto,
        String email,
        PapelUsuarioEnum papel,
        String cref,
        FormacaoEnum formacao,
        String instituicao,
        String cidade,
        String estado,
        boolean ativo,
        LocalDateTime criadoEm
) {
}
