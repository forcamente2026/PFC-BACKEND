package forcamente.api.mapper;

import forcamente.api.dto.UsuarioAdminResponseDTO;
import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.UsuarioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)

    @Mapping(target = "senhaHash", ignore = true)

    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "criadoEm", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "aceitouTermosUsoEm", ignore = true)
    @Mapping(target = "versaoTermosUso", ignore = true)
    @Mapping(target = "aceitouPoliticaPrivacidadeEm", ignore = true)
    @Mapping(target = "versaoPoliticaPrivacidade", ignore = true)
    UsuarioEntity toEntity(UsuarioRequestDTO usuarioRequestDTO);


    UsuarioResponseDTO toDTO(UsuarioEntity usuarioEntity);

    UsuarioAdminResponseDTO toAdminDTO(UsuarioEntity usuarioEntity);
}
