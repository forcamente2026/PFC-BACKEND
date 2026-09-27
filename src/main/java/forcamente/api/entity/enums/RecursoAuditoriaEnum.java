package forcamente.api.entity.enums;

import lombok.Getter;

@Getter
public enum RecursoAuditoriaEnum {

    USUARIO("Usuário"),
    EXERCICIO("Exercício"),
    AUDITORIA("Trilha de auditoria"),
    ARTIGO("Artigo");

    private final String descricao;

    RecursoAuditoriaEnum(String descricao) {
        this.descricao = descricao;
    }
}
