package forcamente.api.dto;

import java.util.UUID;

public record AlternativaResponseDTO(UUID id, String texto, Boolean correta) {
}
