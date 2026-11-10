package forcamente.api.dto;

import forcamente.api.entity.enums.FormacaoEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificacaoPendenteDTO(
        UUID id,
        String nomeCompleto,
        String email,
        String cref,
        FormacaoEnum formacao,
        LocalDateTime criadoEm
) {
}
