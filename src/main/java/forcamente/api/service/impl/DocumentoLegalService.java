package forcamente.api.service.impl;


import forcamente.api.dto.DocumentoLegalResponseDTO;
import forcamente.api.entity.DocumentoLegalEntity;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.mapper.DocumentoLegalMapper;
import forcamente.api.repository.IDocumentoLegalRepository;
import forcamente.api.service.IDocumentoLegalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoLegalService  implements IDocumentoLegalService {

    private final IDocumentoLegalRepository documentoLegalRepository;
    private final DocumentoLegalMapper documentoLegalMapper;


    @Override
    @Transactional(readOnly = true)
    public List<DocumentoLegalResponseDTO> listarVigentes(){
        log.info("listarVigentes");

        return Arrays.stream(TipoDocumentoLegalEnum.values()).map(this::buscarVigente).toList();
    }


    @Override
    @Transactional(readOnly = true)
    public DocumentoLegalResponseDTO buscarVigente(TipoDocumentoLegalEnum tipo) {
        log.info("buscarVigente: {}", tipo);

        return documentoLegalMapper.toDTO(vigente(tipo));
    }

    @Override
    @Transactional(readOnly = true)
    public String versaoVigente(TipoDocumentoLegalEnum tipo){
        return vigente(tipo).getVersao();
    }

    private DocumentoLegalEntity vigente(TipoDocumentoLegalEnum tipo){
        return documentoLegalRepository.findFirstByTipoOrderByVigenteDesdeDesc(tipo).orElseThrow(()-> new RecursoNaoEncontradoException(
                "Nenhuma versao vigente para: " + tipo.getDescricao()
        ));
    }

}
