-- Dados de apresentacao do ForcaMente.
--
-- NAO e migracao: roda a mao, e sai inteiro com o limpar-apresentacao.sql.
-- A marca de tudo que este script cria e o dominio @apresentacao.test.
-- Senha de todas as contas: Senha@123 (o mesmo hash da V3).
--
-- Rodar:  psql -U <usuario> -d forcamente -f scripts/popular-apresentacao.sql
-- Desfazer: psql -U <usuario> -d forcamente -f scripts/limpar-apresentacao.sql

BEGIN;

INSERT INTO usuarios (id, nome_completo, email, senha_hash, papel, data_nascimento,
                      situacao_cref, cref, formacao,
                      cep, logradouro, numero, bairro, cidade, estado,
                      aceitou_termos_uso_em, versao_termos_uso,
                      aceitou_politica_privacidade_em, versao_politica_privacidade,
                      ativo, criado_em)
VALUES (gen_random_uuid(), 'Administrador da Apresentacao', 'admin@apresentacao.test',
        '$2a$10$07bwu4iU3hyYxLL..K6CGePx8a0vtbOr45yxEcf/eZ0K9nHEWClxi',
        'ADMINISTRADOR', DATE '1985-05-20',
        NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        now(), '1.0', now(), '1.0',
        true, now());

INSERT INTO usuarios (id, nome_completo, email, senha_hash, papel, data_nascimento,
                      situacao_cref, cref, formacao,
                      cep, logradouro, numero, bairro, cidade, estado,
                      aceitou_termos_uso_em, versao_termos_uso,
                      aceitou_politica_privacidade_em, versao_politica_privacidade,
                      ativo, criado_em)
SELECT gen_random_uuid(),
       'Professor Apresentacao ' || lpad(i::text, 2, '0'),
       'professor' || lpad(i::text, 2, '0') || '@apresentacao.test',
       '$2a$10$07bwu4iU3hyYxLL..K6CGePx8a0vtbOr45yxEcf/eZ0K9nHEWClxi',
       'PROFESSOR',
       DATE '1990-01-01' + (i * 37),
       'APROVADO',
       (900000 + i)::text || '-G/SP',
       CASE WHEN i % 2 = 0 THEN 'BACHARELADO' ELSE 'LICENCIATURA' END,
       '08780000', 'Rua da Apresentacao', i::text, 'Centro', 'Mogi das Cruzes', 'SP',
       now(), '1.0', now(), '1.0',
       true, now()
FROM generate_series(1, 30) AS i;

INSERT INTO exercicios (id, nome, grupo_muscular, nivel, descricao_execucao,
                        aquecimento_recomendado, erros_comuns, equipamento, gif_url,
                        autor_id, status, criado_em)
SELECT gen_random_uuid(),
       'Exercicio de apresentacao ' || lpad(i::text, 2, '0'),
       (ARRAY['PEITO', 'COSTAS', 'OMBRO', 'BICEPS', 'TRICEPS', 'PERNA', 'GLUTEO', 'ABDOMEN'])[1 + (i % 8)],
       (ARRAY['INICIANTE', 'INTERMEDIARIO', 'AVANCADO'])[1 + (i % 3)],
       'Mantenha a coluna neutra, controle a fase excentrica em tres segundos e '
           || 'respire na subida. Repita o movimento dentro da amplitude confortavel.',
       'Cinco minutos de esteira e uma serie leve de aproximacao.',
       'Perder a neutralidade da coluna e usar impulso no inicio do movimento.',
       'Barra, banco e anilhas',
       'https://exemplo.test/gif/apresentacao-' || i,
       (SELECT id FROM usuarios
        WHERE email = 'professor' || lpad((1 + (i % 30))::text, 2, '0') || '@apresentacao.test'),
       'PENDENTE',
       now()
FROM generate_series(1, 15) AS i;

INSERT INTO artigos (id, titulo, categoria, resumo, fonte, autoria, tipo_estudo, ano,
                     status, motivo_rejeicao, autor_id, criado_em)
SELECT gen_random_uuid(),
       'Artigo de apresentacao ' || lpad(i::text, 2, '0'),
       (ARRAY['FISIOLOGIA', 'BIOMECANICA', 'PERIODIZACAO', 'NUTRICAO'])[1 + (i % 4)],
       repeat('Revisao dos mecanismos de adaptacao ao treinamento de forca, com '
                  || 'enfase em volume semanal e proximidade da falha. ', 4),
       'https://doi.org/10.1519/apresentacao.' || i,
       'Schoenfeld BJ; Grgic J',
       'Revisao sistematica',
       2020 + (i % 5),
       CASE WHEN i <= 10 THEN 'APROVADO'
            WHEN i <= 13 THEN 'PENDENTE'
            ELSE 'REJEITADO' END,
       CASE WHEN i > 13 THEN 'Fonte sem revisao por pares.' END,
       (SELECT id FROM usuarios
        WHERE email = 'professor' || lpad((1 + (i % 30))::text, 2, '0') || '@apresentacao.test'),
       now() - (i * interval '1 hour')
FROM generate_series(1, 15) AS i;

INSERT INTO quizzes (id, titulo, descricao, categoria, nivel, status, motivo_rejeicao,
                     autor_id, criado_em)
SELECT gen_random_uuid(),
       'Quiz de apresentacao ' || lpad(i::text, 2, '0'),
       'Dez perguntas sobre os principios de treinamento abordados na biblioteca.',
       (ARRAY['FISIOLOGIA', 'BIOMECANICA', 'PERIODIZACAO', 'NUTRICAO'])[1 + (i % 4)],
       (ARRAY['INICIANTE', 'INTERMEDIARIO', 'AVANCADO'])[1 + (i % 3)],
       CASE WHEN i <= 7 THEN 'APROVADO'
            WHEN i <= 9 THEN 'PENDENTE'
            ELSE 'REJEITADO' END,
       CASE WHEN i > 9 THEN 'Enunciado da terceira pergunta esta ambiguo.' END,
       (SELECT id FROM usuarios
        WHERE email = 'professor' || lpad((1 + (i % 30))::text, 2, '0') || '@apresentacao.test'),
       now() - (i * interval '1 hour')
FROM generate_series(1, 10) AS i;

INSERT INTO quiz_questoes (id, quiz_id, ordem, enunciado, explicacao)
SELECT gen_random_uuid(),
       q.id,
       p.ordem,
       'Pergunta ' || (p.ordem + 1) || ': qual adaptacao e esperada ao aumentar o volume '
           || 'semanal mantendo a intensidade?',
       'A adaptacao esperada acompanha o aumento do estimulo, dentro da capacidade de recuperacao.'
FROM quizzes q
         CROSS JOIN generate_series(0, 3) AS p(ordem)
WHERE q.titulo LIKE 'Quiz de apresentacao %';

INSERT INTO quiz_alternativas (id, questao_id, ordem, texto, correta)
SELECT gen_random_uuid(),
       qt.id,
       a.ordem,
       'Alternativa ' || chr(65 + a.ordem) || ' da pergunta ' || (qt.ordem + 1),
       a.ordem = 0
FROM quiz_questoes qt
         JOIN quizzes q ON q.id = qt.quiz_id
         CROSS JOIN generate_series(0, 3) AS a(ordem)
WHERE q.titulo LIKE 'Quiz de apresentacao %';

INSERT INTO artigo_aprovacoes (id, artigo_id, professor_id, aprovado_em)
SELECT gen_random_uuid(), a.id, p.id, now()
FROM artigos a
         JOIN LATERAL (SELECT u.id
                       FROM usuarios u
                       WHERE u.email LIKE '%@apresentacao.test'
                         AND u.papel = 'PROFESSOR'
                         AND u.id <> a.autor_id
                       ORDER BY u.email
                       LIMIT 3) p ON true
WHERE a.titulo LIKE 'Artigo de apresentacao %'
  AND a.status = 'APROVADO';

INSERT INTO quiz_aprovacoes (id, quiz_id, professor_id, aprovado_em)
SELECT gen_random_uuid(), q.id, p.id, now()
FROM quizzes q
         JOIN LATERAL (SELECT u.id
                       FROM usuarios u
                       WHERE u.email LIKE '%@apresentacao.test'
                         AND u.papel = 'PROFESSOR'
                         AND u.id <> q.autor_id
                       ORDER BY u.email
                       LIMIT 3) p ON true
WHERE q.titulo LIKE 'Quiz de apresentacao %'
  AND q.status = 'APROVADO';

INSERT INTO registros_auditoria (id, ocorrido_em, acao, recurso_tipo, recurso_id, usuario_id)
SELECT gen_random_uuid(), a.criado_em, 'CRIADO', 'ARTIGO', a.id, a.autor_id
FROM artigos a
WHERE a.titulo LIKE 'Artigo de apresentacao %'
UNION ALL
SELECT gen_random_uuid(), ap.aprovado_em, 'APROVADO', 'ARTIGO', ap.artigo_id, ap.professor_id
FROM artigo_aprovacoes ap
         JOIN artigos a ON a.id = ap.artigo_id
WHERE a.titulo LIKE 'Artigo de apresentacao %'
UNION ALL
SELECT gen_random_uuid(), q.criado_em, 'CRIADO', 'QUIZ', q.id, q.autor_id
FROM quizzes q
WHERE q.titulo LIKE 'Quiz de apresentacao %'
UNION ALL
SELECT gen_random_uuid(), ap.aprovado_em, 'APROVADO', 'QUIZ', ap.quiz_id, ap.professor_id
FROM quiz_aprovacoes ap
         JOIN quizzes q ON q.id = ap.quiz_id
WHERE q.titulo LIKE 'Quiz de apresentacao %'
UNION ALL
SELECT gen_random_uuid(), e.criado_em, 'CRIADO', 'EXERCICIO', e.id, e.autor_id
FROM exercicios e
WHERE e.nome LIKE 'Exercicio de apresentacao %';

COMMIT;

SELECT 'usuarios' AS tabela, count(*) AS criados
FROM usuarios
WHERE email LIKE '%@apresentacao.test'
UNION ALL
SELECT 'exercicios', count(*) FROM exercicios WHERE nome LIKE 'Exercicio de apresentacao %'
UNION ALL
SELECT 'artigos', count(*) FROM artigos WHERE titulo LIKE 'Artigo de apresentacao %'
UNION ALL
SELECT 'quizzes', count(*) FROM quizzes WHERE titulo LIKE 'Quiz de apresentacao %'
UNION ALL
SELECT 'perguntas', count(*)
FROM quiz_questoes qt JOIN quizzes q ON q.id = qt.quiz_id
WHERE q.titulo LIKE 'Quiz de apresentacao %';
