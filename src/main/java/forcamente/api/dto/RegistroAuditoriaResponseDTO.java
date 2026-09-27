package forcamente.api.dto;

import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistroAuditoriaResponseDTO(
        UUID id,
        LocalDateTime ocorridoEm,
        AcaoAuditoriaEnum acao,
        String descricaoAcao,
        RecursoAuditoriaEnum recursoTipo,
        String descricaoRecursoTipo,
        UUID recursoId,
        UUID usuarioId,
        String usuarioNome,
        String usuarioEmail
) {
}
