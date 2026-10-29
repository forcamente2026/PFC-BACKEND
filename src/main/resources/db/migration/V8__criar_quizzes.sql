CREATE TABLE quizzes (
    id              uuid         NOT NULL,
    titulo          varchar(255) NOT NULL,
    descricao       text         NOT NULL,
    categoria       varchar(20)  NOT NULL,
    nivel           varchar(20)  NOT NULL,
    status          varchar(30)  NOT NULL,
    motivo_rejeicao text,
    autor_id        uuid         NOT NULL,
    criado_em       timestamp    NOT NULL,
    atualizado_em   timestamp,

    CONSTRAINT quizzes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_quizzes_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
    CONSTRAINT quizzes_categoria_check
        CHECK (categoria IN ('FISIOLOGIA', 'BIOMECANICA', 'PERIODIZACAO', 'NUTRICAO')),
    CONSTRAINT quizzes_nivel_check
        CHECK (nivel IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO')),
    CONSTRAINT quizzes_status_check
        CHECK (status IN ('PENDENTE', 'APROVADO_PELOS_PROFESSORES', 'APROVADO', 'REJEITADO'))
);

CREATE TABLE quiz_questoes (
    id         uuid    NOT NULL,
    quiz_id    uuid    NOT NULL,
    ordem      integer NOT NULL,
    enunciado  text    NOT NULL,
    explicacao text,

    CONSTRAINT quiz_questoes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_quiz_questoes_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes (id) ON DELETE CASCADE
);

CREATE TABLE quiz_alternativas (
    id         uuid         NOT NULL,
    questao_id uuid         NOT NULL,
    ordem      integer      NOT NULL,
    texto      varchar(255) NOT NULL,
    correta    boolean      NOT NULL,

    CONSTRAINT quiz_alternativas_pkey PRIMARY KEY (id),
    CONSTRAINT fk_quiz_alternativas_questao FOREIGN KEY (questao_id)
        REFERENCES quiz_questoes (id) ON DELETE CASCADE
);

CREATE TABLE quiz_artigos (
    quiz_id   uuid    NOT NULL,
    ordem     integer NOT NULL,
    artigo_id uuid    NOT NULL,

    CONSTRAINT quiz_artigos_pkey PRIMARY KEY (quiz_id, ordem),
    CONSTRAINT fk_quiz_artigos_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_artigos_artigo FOREIGN KEY (artigo_id)
        REFERENCES artigos (id) ON DELETE CASCADE
);

CREATE TABLE quiz_aprovacoes (
    id           uuid      NOT NULL,
    quiz_id      uuid      NOT NULL,
    professor_id uuid      NOT NULL,
    aprovado_em  timestamp NOT NULL,

    CONSTRAINT quiz_aprovacoes_pkey PRIMARY KEY (id),
    CONSTRAINT uk_quiz_aprovacoes UNIQUE (quiz_id, professor_id),
    CONSTRAINT fk_quiz_aprovacoes_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_aprovacoes_professor FOREIGN KEY (professor_id)
        REFERENCES usuarios (id)
);

CREATE TABLE quiz_tentativas (
    id         uuid      NOT NULL,
    quiz_id    uuid      NOT NULL,
    usuario_id uuid      NOT NULL,
    acertos    integer   NOT NULL,
    total      integer   NOT NULL,
    feita_em   timestamp NOT NULL,

    CONSTRAINT quiz_tentativas_pkey PRIMARY KEY (id),
    CONSTRAINT fk_quiz_tentativas_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_tentativas_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id),
    CONSTRAINT quiz_tentativas_acertos_check CHECK (acertos >= 0 AND acertos <= total)
);

CREATE INDEX idx_quiz_tentativas_usuario_quiz ON quiz_tentativas (usuario_id, quiz_id);

ALTER TABLE registros_auditoria DROP CONSTRAINT registros_auditoria_recurso_tipo_check;
ALTER TABLE registros_auditoria ADD CONSTRAINT registros_auditoria_recurso_tipo_check
    CHECK (recurso_tipo IN ('USUARIO', 'EXERCICIO', 'AUDITORIA', 'ARTIGO', 'QUIZ'));
