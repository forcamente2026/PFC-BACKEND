ALTER TABLE usuarios
    ADD COLUMN email_pendente varchar(255);

ALTER TABLE usuarios
DROP CONSTRAINT usuarios_codigo_tipo_check;

ALTER TABLE usuarios
    ADD CONSTRAINT usuarios_codigo_tipo_check
        CHECK (codigo_tipo IN ('MFA', 'REDEFINICAO_SENHA', 'TROCA_DE_EMAIL'));
