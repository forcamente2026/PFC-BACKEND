package forcamente.api.entity.enums;

import lombok.Getter;

@Getter
public enum CategoriaArtigoEnum {
    FISIOLOGIA("Fisiologia"),
    BIOMECANICA("Biomecânica"),
    PERIODIZACAO("Periodização"),
    NUTRICAO("Nutrição");

    private final String descricao;

    CategoriaArtigoEnum(String descricao) {
        this.descricao = descricao;
    }
}
