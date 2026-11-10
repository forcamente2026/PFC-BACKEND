package forcamente.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmarEmailRequestDTO(

        @NotBlank(message = "Informe o codigo")
        @Pattern(regexp = "\\d{4}", message = "O codigo tem 4 digitos")
        String codigo
) {
}
