package forcamente.api.service;

import forcamente.api.dto.DocumentoLegalResponseDTO;
import forcamente.api.entity.DocumentoLegalEntity;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.mapper.DocumentoLegalMapper;
import forcamente.api.repository.IDocumentoLegalRepository;
import forcamente.api.service.impl.DocumentoLegalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoLegalService - versao vigente de cada documento")
class DocumentoLegalServiceTest {

    @Mock
    private IDocumentoLegalRepository documentoLegalRepository;

    @Mock
    private DocumentoLegalMapper documentoLegalMapper;

    @InjectMocks
    private DocumentoLegalService documentoLegalService;

    @Test
    @DisplayName("deve devolver o documento vigente com texto e versao")
    void deveBuscarVigente() {
        var entity = umDocumento(TipoDocumentoLegalEnum.TERMOS_USO, "1.0");
        var dto = umaResposta(entity);

        when(documentoLegalRepository.findFirstByTipoOrderByVigenteDesdeDesc(TipoDocumentoLegalEnum.TERMOS_USO))
                .thenReturn(Optional.of(entity));
        when(documentoLegalMapper.toDTO(entity)).thenReturn(dto);

        var resultado = documentoLegalService.buscarVigente(TipoDocumentoLegalEnum.TERMOS_USO);

        assertThat(resultado.versao()).isEqualTo("1.0");
        assertThat(resultado.descricaoTipo()).isEqualTo("Termos de Uso");
    }

    @Test
    @DisplayName("deve devolver apenas a versao, para o cadastro carimbar o aceite")
    void deveDevolverApenasAVersao() {
        var entity = umDocumento(TipoDocumentoLegalEnum.POLITICA_PRIVACIDADE, "2.0");

        when(documentoLegalRepository.findFirstByTipoOrderByVigenteDesdeDesc(TipoDocumentoLegalEnum.POLITICA_PRIVACIDADE))
                .thenReturn(Optional.of(entity));

        assertThat(documentoLegalService.versaoVigente(TipoDocumentoLegalEnum.POLITICA_PRIVACIDADE))
                .isEqualTo("2.0");
    }

    @Test
    @DisplayName("deve lancar excecao quando nao existe versao vigente")
    void deveLancarExcecaoSemVersaoVigente() {
        when(documentoLegalRepository.findFirstByTipoOrderByVigenteDesdeDesc(TipoDocumentoLegalEnum.TERMOS_USO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoLegalService.buscarVigente(TipoDocumentoLegalEnum.TERMOS_USO))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Nenhuma versao vigente");
    }

    private DocumentoLegalEntity umDocumento(TipoDocumentoLegalEnum tipo, String versao) {
        var entity = new DocumentoLegalEntity();
        entity.setId(UUID.randomUUID());
        entity.setTipo(tipo);
        entity.setVersao(versao);
        entity.setTexto("Texto a definir.");
        entity.setVigenteDesde(LocalDateTime.now());
        return entity;
    }

    private DocumentoLegalResponseDTO umaResposta(DocumentoLegalEntity entity) {
        return new DocumentoLegalResponseDTO(
                entity.getTipo(),
                entity.getTipo().getDescricao(),
                entity.getVersao(),
                entity.getTexto(),
                entity.getVigenteDesde());
    }
}