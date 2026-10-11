package forcamente.api.dto;

import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;

public record HidratacaoRequestDTO(

        @NotNull(message = "Informe o peso")
        @DecimalMin(value = "20.0", message = "O peso deve ser de no mínimo 20 kg")

        @DecimalMax(value = "300", message = "O peso deve ser de no máximo 300 kg")
        double pesokg,

        @NotNull(message = "Informe os minutos de treino por dia")
        @Min(value = 0, message = "Os minutos de treino não podem ser negativos")
        @Max(value = 480, message = "Os minutos de treino devem ser de no maximo 480")
        Integer minutosPorDia
) {
}
