package forcamente.api.entity.enums;

import lombok.Getter;

@Getter
public enum TipoCodigoEnum {

    MFA("Segundo fator de autenticacao"),
    REDEFINICAO_SENHA("Redefinicao de senha"),
    TROCA_DE_EMAIL("Troca de e-mail");

    private final String descricao;

    TipoCodigoEnum(String descricao) {
        this.descricao = descricao;
    }
}


