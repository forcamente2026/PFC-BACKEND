package forcamente.api.service;

import forcamente.api.dto.DocumentoLegalResponseDTO;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;

import java.util.List;

public interface IDocumentoLegalService {
    List<DocumentoLegalResponseDTO> listarVigentes();
    DocumentoLegalResponseDTO buscarVigente(TipoDocumentoLegalEnum tipo);
    String versaoVigente(TipoDocumentoLegalEnum tipo);
}
