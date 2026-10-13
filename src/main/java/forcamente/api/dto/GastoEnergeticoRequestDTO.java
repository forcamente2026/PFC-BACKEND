package forcamente.api.dto;

import forcamente.api.entity.enums.NivelAtividadeEnum;
import forcamente.api.entity.enums.SexoBiologicoEnum;
import jakarta.validation.constraints.*;

public record GastoEnergeticoRequestDTO(
        @NotNull(message = "Informe o peso")
        @DecimalMin(value = "20", message = "O peso deve ser no mínimo 20kg")
        @DecimalMax(value = "300", message = "O peso deve ser de no máximo 300kg")
        Double pesoKg,

        @NotNull(message = "Informe a altura")
        @Min(value = 100, message = "A altura deve ser de no mínimo 100 cm")
        @Max(value = 250, message = "A altura deve ser de no máximo 250 cm")
        Integer alturaCm,

        @NotNull(message = "Informe a idade")
        @Min(value = 18, message = "A plataforma é restrita a maiores de 18 anos")

        @Max(value = 120, message = "A idade deve ser de no máximo 120 anos")
        Integer idade,

        @NotNull(message = "Informe o sexo biológico")
        SexoBiologicoEnum sexoBiologico,

        @NotNull(message = "Informe o nível de atividade física")
        NivelAtividadeEnum nivelAtividade

) {
}
