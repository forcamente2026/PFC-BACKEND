package forcamente.api.entity.enums;

import lombok.Getter;

@Getter
public enum AcaoAuditoriaEnum {

    CRIADO("Criado"),
    LIDO("Lido"),
    ATUALIZADO("Atualizado"),
    EXCLUIDO("Excluído"),
    LOGIN_REALIZADO("Login realizado"),
    LOGIN_FALHOU("Falha no login"),
    MFA_FALHOU("Falha na verificação do código"),
    ANONIMIZADO("Conta anonimizada"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado");

    private final String descricao;

    AcaoAuditoriaEnum(String descricao) {
        this.descricao = descricao;
    }
}
