ALTER TABLE usuarios
    ADD COLUMN senha_falhas_janela      integer,
    ADD COLUMN senha_janela_inicio      timestamp,
    ADD COLUMN senha_erros_consecutivos integer,
    ADD COLUMN senha_pausa_ocorrencias  integer,
    ADD COLUMN senha_bloqueado_ate      timestamp;

ALTER TABLE registros_auditoria
DROP CONSTRAINT registros_auditoria_acao_check;

ALTER TABLE registros_auditoria
    ADD CONSTRAINT registros_auditoria_acao_check
        CHECK (acao IN ('CRIADO', 'LIDO', 'ATUALIZADO', 'EXCLUIDO',
                        'LOGIN_REALIZADO', 'LOGIN_FALHOU', 'LOGIN_BLOQUEADO',
                        'MFA_FALHOU', 'ANONIMIZADO', 'APROVADO', 'REJEITADO'));