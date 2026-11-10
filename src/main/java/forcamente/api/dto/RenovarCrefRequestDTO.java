package forcamente.api.dto;

import forcamente.api.entity.enums.FormacaoEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RenovarCrefRequestDTO(

        @NotBlank(message = "Informe o CREF")
        @Pattern(regexp = "\\d{6}-[A-Z]/[A-Z]{2}", message = "O CREF deve estar no formato 123456-G/SP")
        String cref,

        @NotNull(message = "Informe a formação")
        FormacaoEnum formacao
) {
        public RenovarCrefRequestDTO {
                cref = cref == null ? null : cref.trim().toUpperCase();
        }
}
