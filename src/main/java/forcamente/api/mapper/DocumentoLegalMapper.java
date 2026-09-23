package forcamente.api.mapper;


import forcamente.api.dto.DocumentoLegalResponseDTO;
import forcamente.api.entity.DocumentoLegalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentoLegalMapper {

    @Mapping(target = "descricaoTipo", expression = "java(documentoLegalEntity.getTipo().getDescricao())")
    DocumentoLegalResponseDTO toDTO(DocumentoLegalEntity documentoLegalEntity);
}
