package forcamente.api.config;


import forcamente.api.entity.DocumentoLegalEntity;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.repository.IDocumentoLegalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SemeadorDocumentosLegais implements ApplicationRunner {
    private static final String VERSAO_INICIAL = "1.0";

    private static final String TEXTO_PROVISORIO = "Texto a definir";

    private final IDocumentoLegalRepository documentoLegalRepository;

    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (TipoDocumentoLegalEnum tipo : TipoDocumentoLegalEnum.values()) {
            if (documentoLegalRepository.existsByTipo(tipo)) {
                continue;
            }

            var documento = new DocumentoLegalEntity();
            documento.setTipo(tipo);
            documento.setVersao(VERSAO_INICIAL);
            documento.setTexto(TEXTO_PROVISORIO);
            documento.setVigenteDesde(LocalDateTime.now(clock));

            documentoLegalRepository.save(documento);
            log.info("Documento legal semeado: {} versao {}", tipo, VERSAO_INICIAL);
        }
    }
}
