ALTER TABLE exercicios ADD COLUMN autor_id uuid;
ALTER TABLE exercicios ADD CONSTRAINT fk_exercicios_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id);
ALTER TABLE exercicios ADD COLUMN status varchar(30) NOT NULL DEFAULT 'APROVADO';
ALTER TABLE exercicios ALTER COLUMN status DROP DEFAULT;
ALTER TABLE exercicios ADD CONSTRAINT exercicios_status_check CHECK (status IN ('PENDENTE', 'APROVADO_PELOS_PROFESSORES', 'APROVADO', 'REJEITADO'));
ALTER TABLE exercicios ADD COLUMN motivo_rejeicao text;

CREATE TABLE exercicio_aprovacoes (
    id           uuid      NOT NULL,
    exercicio_id uuid      NOT NULL,
    professor_id uuid      NOT NULL,
    aprovado_em  timestamp NOT NULL,

    CONSTRAINT exercicio_aprovacoes_pkey PRIMARY KEY (id),
    CONSTRAINT uk_exercicio_aprovacoes UNIQUE (exercicio_id, professor_id),
    CONSTRAINT fk_exercicio_aprovacoes_exercicio FOREIGN KEY (exercicio_id) REFERENCES exercicios (id) ON DELETE CASCADE,
    CONSTRAINT fk_exercicio_aprovacoes_professor FOREIGN KEY (professor_id) REFERENCES usuarios (id)
);
