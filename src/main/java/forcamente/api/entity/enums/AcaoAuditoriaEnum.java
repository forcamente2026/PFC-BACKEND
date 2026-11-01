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
    LOGIN_BLOQUEADO("Login bloqueado por tentativas"),
    MFA_FALHOU("Falha na verificação do código"),
    SENHA_FALHOU("Senha atual errada na troca de senha"),
    ANONIMIZADO("Conta anonimizada"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado");


    private final String descricao;

    AcaoAuditoriaEnum(String descricao) {
        this.descricao = descricao;
    }
}
