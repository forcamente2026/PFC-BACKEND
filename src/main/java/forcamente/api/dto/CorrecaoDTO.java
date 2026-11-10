package forcamente.api.dto;

import java.util.UUID;

public record CorrecaoDTO(UUID questaoId,
                          UUID alternativaCorretaId,
                          UUID alternativaEscolhidaId,
                          boolean acertou,
                          String explicacao) {
}
