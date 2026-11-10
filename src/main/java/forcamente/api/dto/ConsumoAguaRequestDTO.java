package forcamente.api.dto;

import jakarta.validation.constraints.*;

public record ConsumoAguaRequestDTO(

        @NotNull(message = "Informe o peso")
        @DecimalMin(value = "20.0", message = "O peso deve ser de no mínimo 20 kg")
        @DecimalMax(value = "300", message = "O peso deve ser de no máximo 300 kg")
        Double pesoKg,

        @NotNull(message = "Informe os minutos de treino por dia")
        @Min(value = 0, message = "Os minutos de treino não podem ser negativos")
        @Max(value = 480, message = "Os minutos de treino devem ser de no maximo 480")
        Integer minutosTreinoPorDia,

        @NotNull(message = "Informe o total ja consumido no dia")
        @Min(value = 0, message = "O total consumido no dia não pode ser negativo")
        @Max(value = 20000, message = "O total consumido no dia deve ser de no maximo 20000 ml")
        Integer totalConsumidoMl,

        @Min(value = 0, message = "A quantidade não pode ser negativa")
        @Max(value = 5000, message = "A quantidade de um registro deve ser de no maximo 5000 ml")
        Integer quantidadeMl
) {

        public ConsumoAguaRequestDTO {
                quantidadeMl = quantidadeMl == null ? 0 : quantidadeMl;
        }
}
