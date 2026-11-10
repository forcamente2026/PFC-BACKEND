package forcamente.api.entity.enums;


import lombok.Getter;

@Getter
public enum SexoBiologicoEnum {
    MASCULINO("Masculino"),
    FEMININO("Feminino");

    private final String descricao;

    SexoBiologicoEnum(String descricao) {
        this.descricao = descricao;
    }
}
