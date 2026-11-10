ALTER TABLE usuarios
    ADD COLUMN situacao_cref        varchar(20),
    ADD COLUMN motivo_rejeicao_cref text,
    ADD COLUMN cref_reenvios        integer,
    ADD COLUMN cref_reenvio_dia     date;

UPDATE usuarios SET situacao_cref = 'APROVADO' WHERE papel = 'PROFESSOR';

ALTER TABLE usuarios
    ADD CONSTRAINT usuarios_situacao_cref_check
        CHECK (situacao_cref IN ('PENDENTE', 'APROVADO', 'REJEITADO'));
