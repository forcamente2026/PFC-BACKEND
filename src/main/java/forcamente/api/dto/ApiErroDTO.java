package forcamente.api.dto;

import java.util.List;

public record ApiErroDTO(int status, String message, List<CampoErroDTO> campos) {
}
