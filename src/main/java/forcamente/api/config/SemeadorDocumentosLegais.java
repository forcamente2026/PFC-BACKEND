package forcamente.api.config;


import forcamente.api.entity.DocumentoLegalEntity;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.repository.IDocumentoLegalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SemeadorDocumentosLegais implements ApplicationRunner {

    private static final String VERSAO_INICIAL = "1.0";

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
            documento.setTexto(lerArquivo(arquivoDe(tipo)));
            documento.setVigenteDesde(LocalDateTime.now(clock));

            documentoLegalRepository.save(documento);
            log.info("Documento legal semeado: {} versao {}", tipo, VERSAO_INICIAL);
        }
    }

    private String arquivoDe(TipoDocumentoLegalEnum tipo) {
        return switch (tipo) {
            case TERMOS_USO -> "documentos/termos-uso-" + VERSAO_INICIAL + ".json";
            case POLITICA_PRIVACIDADE -> "documentos/politica-privacidade-" + VERSAO_INICIAL + ".json";
        };
    }

    private String lerArquivo(String caminho) {
        try (InputStream entrada = new ClassPathResource(caminho).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException excecao) {
            throw new IllegalStateException("Nao foi possivel ler o documento legal: " + caminho, excecao);
        }
    }
}
