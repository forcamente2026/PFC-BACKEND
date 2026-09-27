package forcamente.api.entity.enums;

import lombok.Getter;

@Getter
public enum FormacaoEnum {
    BACHARELADO("Bacharelado em Educação Física"),
    LICENCIATURA("Licenciatura em Educação Física");

    private final String descricao;

    FormacaoEnum(String descricao) {
        this.descricao = descricao;
    }
}
