package forcamente.api.dto;

import java.util.List;
import java.util.UUID;

public record QuestaoResponseDTO(UUID id,
                                 String enunciado,
                                 String explicacao,
                                 List<AlternativaResponseDTO> alternativas) {
}
