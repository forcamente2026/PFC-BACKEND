
CREATE TABLE usuarios (
    id                              uuid         NOT NULL,
    nome_completo                   varchar(255) NOT NULL,
    email                           varchar(255) NOT NULL,
    senha_hash                      varchar(255) NOT NULL,
    papel                           varchar(20)  NOT NULL,
    data_nascimento                 date         NOT NULL,

    cref                            varchar(11),
    formacao                        varchar(20),

    aceitou_termos_uso_em           timestamp,
    versao_termos_uso               varchar(10),
    aceitou_politica_privacidade_em timestamp,
    versao_politica_privacidade     varchar(10),

    anonimizacao_solicitada_em      timestamp,
    anonimizado_em                  timestamp,

    codigo_hash                     varchar(60),
    codigo_tipo                     varchar(20),
    codigo_expira_em                timestamp,
    codigo_tentativas               integer,

    mfa_pedidos                     integer,
    mfa_janela_inicio               timestamp,
    mfa_bloqueado_ate               timestamp,
    mfa_ocorrencias_bloqueio        integer,

    redefinicao_pedidos             integer,
    redefinicao_dia                 date,
    redefinicao_bloqueado_ate       timestamp,
    redefinicao_erros_consecutivos  integer,

    cep                             varchar(8),
    logradouro                      varchar(255),
    numero                          varchar(10),
    complemento                     varchar(255),
    bairro                          varchar(255),
    cidade                          varchar(255),
    estado                          varchar(2),

    ativo                           boolean      NOT NULL,
    criado_em                       timestamp    NOT NULL,

    CONSTRAINT usuarios_pkey PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_cref  UNIQUE (cref),
    CONSTRAINT usuarios_papel_check
        CHECK (papel IN ('ALUNO', 'PROFESSOR', 'ADMINISTRADOR')),
    CONSTRAINT usuarios_formacao_check
        CHECK (formacao IN ('BACHARELADO', 'LICENCIATURA')),
    CONSTRAINT usuarios_codigo_tipo_check
        CHECK (codigo_tipo IN ('MFA', 'REDEFINICAO_SENHA'))
);

CREATE TABLE exercicios (
    id                      uuid         NOT NULL,
    nome                    varchar(255) NOT NULL,
    grupo_muscular          varchar(30)  NOT NULL,
    nivel                   varchar(20)  NOT NULL,
    descricao_execucao      text         NOT NULL,
    aquecimento_recomendado text,
    erros_comuns            text,
    equipamento             varchar(255),
    gif_url                 varchar(255),
    criado_em               timestamp    NOT NULL,
    atualizado_em           timestamp,

    CONSTRAINT exercicios_pkey PRIMARY KEY (id),
    CONSTRAINT uk_exercicios_nome UNIQUE (nome),
    CONSTRAINT exercicios_grupo_muscular_check
        CHECK (grupo_muscular IN ('PEITO', 'COSTAS', 'OMBRO', 'BICEPS',
                                  'TRICEPS', 'PERNA', 'GLUTEO', 'ABDOMEN')),
    CONSTRAINT exercicios_nivel_check
        CHECK (nivel IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO'))
);

CREATE TABLE documentos_legais (
    id            uuid         NOT NULL,
    tipo          varchar(30)  NOT NULL,
    versao        varchar(10)  NOT NULL,
    texto         text         NOT NULL,
    vigente_desde timestamp    NOT NULL,

    CONSTRAINT documentos_legais_pkey PRIMARY KEY (id),
    CONSTRAINT uk_documentos_legais_tipo_versao UNIQUE (tipo, versao),
    CONSTRAINT documentos_legais_tipo_check
        CHECK (tipo IN ('TERMOS_USO', 'POLITICA_PRIVACIDADE'))
);

CREATE TABLE registros_auditoria (
    id           uuid        NOT NULL,
    acao         varchar(30) NOT NULL,
    recurso_tipo varchar(20),
    recurso_id   uuid,
    usuario_id   uuid,
    ocorrido_em  timestamp   NOT NULL,

    CONSTRAINT registros_auditoria_pkey PRIMARY KEY (id),
    CONSTRAINT registros_auditoria_acao_check
        CHECK (acao IN ('CRIADO', 'LIDO', 'ATUALIZADO', 'EXCLUIDO',
                        'LOGIN_REALIZADO', 'LOGIN_FALHOU', 'MFA_FALHOU',
                        'ANONIMIZADO', 'APROVADO', 'REJEITADO')),
    CONSTRAINT registros_auditoria_recurso_tipo_check
        CHECK (recurso_tipo IN ('USUARIO', 'EXERCICIO', 'AUDITORIA', 'ARTIGO'))
);

CREATE INDEX idx_auditoria_ocorrido_em ON registros_auditoria (ocorrido_em);
