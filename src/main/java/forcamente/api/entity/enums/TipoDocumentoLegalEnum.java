package forcamente.api.entity.enums;

import lombok.Getter;


@Getter
public enum TipoDocumentoLegalEnum {

    TERMOS_USO("Termos de Uso"),
    POLITICA_PRIVACIDADE("Política de Privacidade");

    private final String descricao;

    TipoDocumentoLegalEnum(String descricao) {
        this.descricao = descricao;
    }
}
