package forcamente.api.dto;

import forcamente.api.entity.enums.TipoDocumentoLegalEnum;

import java.time.LocalDateTime;

public record DocumentoLegalResponseDTO(
        TipoDocumentoLegalEnum tipo,
        String descricaoTipo,
        String versao,
        String texto,
        LocalDateTime vigenteDesde
) {
}
