
ALTER TABLE usuarios DROP COLUMN IF EXISTS cpf;

ALTER TABLE usuarios DROP COLUMN IF EXISTS instituicao;

ALTER TABLE usuarios DROP COLUMN IF EXISTS categoria_profissional;

ALTER TABLE usuarios ALTER COLUMN data_nascimento SET NOT NULL;
