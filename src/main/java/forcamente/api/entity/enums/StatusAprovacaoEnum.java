package forcamente.api.entity.enums;

import lombok.Getter;

@Getter
public enum StatusAprovacaoEnum {

    PENDENTE("Pendente"),
    APROVADO_PELOS_PROFESSORES("Aprovado pelos professores"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado");

    private final String descricao;

    StatusAprovacaoEnum(String descricao) {
        this.descricao = descricao;
    }
}