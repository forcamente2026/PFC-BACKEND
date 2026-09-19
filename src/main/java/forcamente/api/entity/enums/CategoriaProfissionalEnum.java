package forcamente.api.entity.enums;

import lombok.Getter;

@Getter

public enum CategoriaProfissionalEnum {
    ACADEMIAS_E_CENTROS_DE_TREINAMENTO("Academias e Centros de Treinamento"),
    TREINAMENTO_ESPORTIVO("Treinamento Esportivo"),
    SAUDE_E_REABILITACAO("Saúde e Reabilitação"),
    ATIVIDADES_PARA_GRUPOS_ESPECIAIS("Atividades para Grupos Especiais"),
    CONSULTORIA_E_PERSONAL_TRAINING("Consultoria e Personal Training");

    private final String descricao;

    CategoriaProfissionalEnum(String descricao) {
        this.descricao = descricao;
    }
}
