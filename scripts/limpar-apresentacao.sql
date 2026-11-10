-- Desfaz o popular-apresentacao.sql.
--
-- Apaga na ordem das chaves estrangeiras, e apaga tambem o que as contas de
-- apresentacao fizeram durante a gravacao: tentativas de quiz e aprovacoes
-- dadas em conteudo que nao veio deste script.
--
-- Rodar: psql -U <usuario> -d forcamente -f scripts/limpar-apresentacao.sql

BEGIN;

CREATE TEMPORARY TABLE contas_de_apresentacao AS
SELECT id FROM usuarios WHERE email LIKE '%@apresentacao.test';

DELETE FROM quiz_tentativas
WHERE usuario_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM artigo_aprovacoes
WHERE professor_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM quiz_aprovacoes
WHERE professor_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM exercicio_aprovacoes
WHERE professor_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM quizzes
WHERE titulo LIKE 'Quiz de apresentacao %'
   OR autor_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM artigos
WHERE titulo LIKE 'Artigo de apresentacao %'
   OR autor_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM exercicios
WHERE nome LIKE 'Exercicio de apresentacao %'
   OR autor_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM registros_auditoria
WHERE usuario_id IN (SELECT id FROM contas_de_apresentacao);

DELETE FROM usuarios
WHERE id IN (SELECT id FROM contas_de_apresentacao);

DROP TABLE contas_de_apresentacao;

COMMIT;

SELECT 'contas restantes' AS conferencia, count(*) AS total
FROM usuarios
WHERE email LIKE '%@apresentacao.test'
UNION ALL
SELECT 'artigos restantes', count(*) FROM artigos WHERE titulo LIKE 'Artigo de apresentacao %'
UNION ALL
SELECT 'quizzes restantes', count(*) FROM quizzes WHERE titulo LIKE 'Quiz de apresentacao %'
UNION ALL
SELECT 'exercicios restantes', count(*) FROM exercicios WHERE nome LIKE 'Exercicio de apresentacao %';
