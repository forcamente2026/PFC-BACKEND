package forcamente.api.dto;

import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;

import java.util.List;
import java.util.UUID;

public record QuizEdicaoResponseDTO(
        UUID id,
        String titulo,
        String descricao,
        CategoriaArtigoEnum categoria,
        NivelDificuldadeEnum nivel,
        List<UUID> artigos,
        List<QuestaoRequestDTO> questoes
) {
}
