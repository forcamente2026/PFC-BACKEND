package forcamente.api.controller;


import forcamente.api.dto.DocumentoLegalResponseDTO;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.service.IDocumentoLegalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/api/documentos-legais")
@RequiredArgsConstructor
public class DocumentoLegalController {

    private final IDocumentoLegalService documentoLegalService;

    @GetMapping
    public ResponseEntity<List<DocumentoLegalResponseDTO>> listarVigentes(){
        return ResponseEntity.ok(documentoLegalService.listarVigentes());
    }

    @GetMapping("/{tipo}")
    public ResponseEntity<DocumentoLegalResponseDTO> buscarVigente(
            @PathVariable("tipo")TipoDocumentoLegalEnum tipo) {
        return ResponseEntity.ok(documentoLegalService.buscarVigente(tipo));
    }

}
