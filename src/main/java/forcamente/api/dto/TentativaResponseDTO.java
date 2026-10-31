package forcamente.api.dto;

import java.util.List;

public record TentativaResponseDTO(int acertos,
                                   int total,
                                   int percentual,
                                   boolean concluido,
                                   int notaMinima,
                                   List<CorrecaoDTO> correcao) {
}
