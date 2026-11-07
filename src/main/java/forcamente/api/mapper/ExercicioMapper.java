package forcamente.api.mapper;

import forcamente.api.dto.ExercicioRequestDTO;
import forcamente.api.entity.ExercicioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExercicioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "autorId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "motivoRejeicao", ignore = true)
    ExercicioEntity toEntity(ExercicioRequestDTO exercicioRequestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "grupoMuscular", ignore = true)
    @Mapping(target = "autorId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "motivoRejeicao", ignore = true)
    @Mapping(target = "atualizadoEm", expression = "java(java.time.LocalDateTime.now())")
    void atualizarEntity(ExercicioRequestDTO exercicioRequestDTO, @MappingTarget ExercicioEntity exercicioEntity);
}
