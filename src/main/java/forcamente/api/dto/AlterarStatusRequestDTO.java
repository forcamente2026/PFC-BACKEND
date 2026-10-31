package forcamente.api.dto;

import forcamente.api.entity.enums.StatusAprovacaoEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlterarStatusRequestDTO(
        @NotNull(message = "Informe a situação")
        StatusAprovacaoEnum status,

        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
        String motivo
) {
    @AssertTrue(message = "A situação deve ser Aprovado ou Rejeitado")
    public boolean isSituacaoDecidivel() {
        return status == null || status == StatusAprovacaoEnum.APROVADO || status == StatusAprovacaoEnum.REJEITADO;
    }

    @AssertTrue(message = "Informe o motivo da rejeição")
    public boolean isMotivoPresenteNaRejeicao() {
        return status != StatusAprovacaoEnum.REJEITADO || (motivo != null && !motivo.isBlank());
    }
}
