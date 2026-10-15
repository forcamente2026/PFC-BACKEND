CREATE TABLE artigos (
                         id              uuid         NOT NULL,
                         titulo          varchar(255) NOT NULL,
                         categoria       varchar(20)  NOT NULL,
                         resumo          text         NOT NULL,
                         fonte           varchar(500) NOT NULL,
                         autoria         varchar(255),
                         tipo_estudo     varchar(255),
                         ano             integer,
                         minutos_leitura integer,
                         status          varchar(30)  NOT NULL,
                         motivo_rejeicao text,
                         autor_id        uuid         NOT NULL,
                         criado_em       timestamp    NOT NULL,

                         CONSTRAINT artigos_pkey PRIMARY KEY (id),
                         CONSTRAINT uk_artigos_fonte UNIQUE (fonte),
                         CONSTRAINT fk_artigos_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
                         CONSTRAINT artigos_categoria_check
                             CHECK (categoria IN ('FISIOLOGIA', 'BIOMECANICA', 'PERIODIZACAO', 'NUTRICAO')),
                         CONSTRAINT artigos_status_check
                             CHECK (status IN ('PENDENTE', 'APROVADO_PELOS_PROFESSORES', 'APROVADO', 'REJEITADO')),
                         CONSTRAINT artigos_fonte_https_check
                             CHECK (fonte LIKE 'https://%')
);

CREATE TABLE artigo_aprovacoes (
                                   id           uuid      NOT NULL,
                                   artigo_id    uuid      NOT NULL,
                                   professor_id uuid      NOT NULL,
                                   aprovado_em  timestamp NOT NULL,

                                   CONSTRAINT artigo_aprovacoes_pkey PRIMARY KEY (id),
                                   CONSTRAINT uk_artigo_aprovacoes UNIQUE (artigo_id, professor_id),
                                   CONSTRAINT fk_artigo_aprovacoes_artigo FOREIGN KEY (artigo_id)
                                       REFERENCES artigos (id) ON DELETE CASCADE,
                                   CONSTRAINT fk_artigo_aprovacoes_professor FOREIGN KEY (professor_id)
                                       REFERENCES usuarios (id)
);