ALTER TABLE registros_auditoria
DROP CONSTRAINT registros_auditoria_acao_check;

ALTER TABLE registros_auditoria
    ADD CONSTRAINT registros_auditoria_acao_check
        CHECK (acao IN ('CRIADO', 'LIDO', 'ATUALIZADO', 'EXCLUIDO',
                        'LOGIN_REALIZADO', 'LOGIN_FALHOU', 'LOGIN_BLOQUEADO',
                        'MFA_FALHOU', 'SENHA_FALHOU',
                        'ANONIMIZADO', 'APROVADO', 'REJEITADO'));
