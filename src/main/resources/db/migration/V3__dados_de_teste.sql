
INSERT INTO usuarios (
    id, nome_completo, email, senha_hash, papel, data_nascimento,
    cref, formacao,
    cep, logradouro, numero, complemento, bairro, cidade, estado,
    aceitou_termos_uso_em, versao_termos_uso,
    aceitou_politica_privacidade_em, versao_politica_privacidade,
    anonimizacao_solicitada_em, ativo, criado_em)
SELECT
    gen_random_uuid(),
    d.nome,
    d.email,
    '$2a$10$07bwu4iU3hyYxLL..K6CGePx8a0vtbOr45yxEcf/eZ0K9nHEWClxi',
    d.papel,
    d.nascimento,
    d.cref,
    d.formacao,
    d.cep, d.logradouro, d.numero, NULL, d.bairro, d.cidade, d.estado,
    now() - make_interval(days => d.dias), '1.0',
    now() - make_interval(days => d.dias), '1.0',
    d.exclusao_pedida,
    d.ativo,
    now() - make_interval(days => d.dias)
FROM (VALUES
    ('Adriana Paiva Coelho',     'adriana.coelho@exemplo.test',  'PROFESSOR'::varchar(20), DATE '1985-02-11',
     '100001-G/SP'::varchar(11), 'BACHARELADO'::varchar(20),
     '08773000'::varchar(8), 'Rua Cabo Diogo Oliver'::varchar(255), '120'::varchar(10),
     'Vila Mogilar'::varchar(255), 'Mogi das Cruzes'::varchar(255), 'SP'::varchar(2),
     180, true, NULL::timestamp),

    ('Caio Figueiredo Nunes',    'caio.nunes@exemplo.test',      'PROFESSOR', DATE '1990-07-23',
     '100002-G/SP', 'LICENCIATURA',
     '08674000', 'Rua Benjamin Constant', '45',
     'Centro', 'Suzano', 'SP',
     165, true, NULL),

    ('Debora Salles Vieira',     'debora.vieira@exemplo.test',   'PROFESSOR', DATE '1988-11-04',
     '100003-G/SP', 'BACHARELADO',
     '01310100', 'Avenida Paulista', '1578',
     'Bela Vista', 'Sao Paulo', 'SP',
     150, true, NULL),

    ('Everton Machado Reis',     'everton.reis@exemplo.test',    'PROFESSOR', DATE '1992-05-30',
     '100004-G/SP', 'LICENCIATURA',
     '07010000', 'Rua Dom Pedro II', '300',
     'Centro', 'Guarulhos', 'SP',
     140, false, NULL),

    ('Fernanda Queiroz Dutra',   'fernanda.dutra@exemplo.test',  'PROFESSOR', DATE '1987-09-17',
     '100005-G/SP', 'BACHARELADO',
     '08773000', 'Rua Cabo Diogo Oliver', '88',
     'Vila Mogilar', 'Mogi das Cruzes', 'SP',
     120, true, NULL),

    ('Gustavo Peixoto Amaral',   'gustavo.amaral@exemplo.test',  'PROFESSOR', DATE '1994-01-08',
     '100006-G/SP', 'LICENCIATURA',
     '08674000', 'Rua Benjamin Constant', '910',
     'Centro', 'Suzano', 'SP',
     110, true, NULL),

    ('Ana Beatriz Ramos',        'ana.ramos@exemplo.test',       'ALUNO', DATE '1998-03-12',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 95, true, NULL),

    ('Bruno Carvalho Lima',      'bruno.lima@exemplo.test',      'ALUNO', DATE '2001-06-25',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 92, true, NULL),

    ('Camila Souza Prado',       'camila.prado@exemplo.test',    'ALUNO', DATE '1996-12-02',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 88, true, NULL),

    ('Diego Nogueira Alves',     'diego.alves@exemplo.test',     'ALUNO', DATE '1999-08-19',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 84, false, NULL),

    ('Eduarda Martins Rocha',    'eduarda.rocha@exemplo.test',   'ALUNO', DATE '2000-04-07',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 80, true, NULL),

    ('Felipe Andrade Pinto',     'felipe.pinto@exemplo.test',    'ALUNO', DATE '1997-10-14',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 76, true, NULL),

    ('Gabriela Teixeira Luz',    'gabriela.luz@exemplo.test',    'ALUNO', DATE '2002-02-28',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 70, true, NULL),

    ('Henrique Barbosa Dias',    'henrique.dias@exemplo.test',   'ALUNO', DATE '1995-05-09',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 64, true, NULL),

    ('Isabela Moreira Campos',   'isabela.campos@exemplo.test',  'ALUNO', DATE '1993-07-21',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 58, true, now() - make_interval(days => 3)),

    ('Joao Vitor Siqueira',      'joao.siqueira@exemplo.test',   'ALUNO', DATE '2001-11-30',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 52, true, NULL),

    ('Larissa Fonseca Melo',     'larissa.melo@exemplo.test',    'ALUNO', DATE '1999-01-16',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 46, true, NULL),

    ('Marcelo Antunes Braga',    'marcelo.braga@exemplo.test',   'ALUNO', DATE '1994-09-03',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 40, false, NULL),

    ('Natalia Ribeiro Costa',    'natalia.costa@exemplo.test',   'ALUNO', DATE '2003-03-27',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 33, true, NULL),

    ('Otavio Mendes Faria',      'otavio.faria@exemplo.test',    'ALUNO', DATE '1996-06-11',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 27, true, NULL)
) AS d(nome, email, papel, nascimento,
       cref, formacao,
       cep, logradouro, numero, bairro, cidade, estado,
       dias, ativo, exclusao_pedida)
ON CONFLICT (email) DO NOTHING;


INSERT INTO exercicios (
    id, nome, grupo_muscular, nivel,
    descricao_execucao, aquecimento_recomendado, erros_comuns, equipamento,
    criado_em)
SELECT
    gen_random_uuid(),
    d.nome, d.grupo, d.nivel,
    d.execucao, d.aquecimento, d.erros, d.equipamento,
    now() - make_interval(days => d.dias)
FROM (VALUES
    ('Supino reto com barra', 'PEITO'::varchar(30), 'INTERMEDIARIO'::varchar(20),
     'Deite no banco com os pes apoiados no chao e as escapulas retraidas. Segure a barra um pouco mais aberto que a largura dos ombros, desca controlando ate tocar de leve o peito e empurre de volta sem travar os cotovelos.',
     'Duas series leves de 12 repeticoes com metade da carga, mais rotacao de ombros.'::varchar(255),
     'Tirar os pes do chao, quicar a barra no peito e abrir demais os cotovelos.'::varchar(255),
     'Barra e banco reto'::varchar(255), 60),

    ('Crucifixo com halteres', 'PEITO', 'INICIANTE',
     'Deitado no banco, bracos abertos com leve flexao fixa nos cotovelos. Abra ate sentir o alongamento do peitoral e feche desenhando um arco, sem deixar os halteres se tocarem.',
     'Elevacoes laterais leves para aquecer o ombro.', 'Flexionar e estender o cotovelo, transformando o exercicio num supino.',
     'Halteres e banco reto', 58),

    ('Puxada frontal na polia', 'COSTAS', 'INICIANTE',
     'Sentado, joelhos presos no apoio, pegada aberta. Puxe a barra ate a altura da clavicula levando os cotovelos para baixo e para tras, e suba controlando.',
     'Uma serie leve de 15 repeticoes na propria polia.', 'Jogar o tronco para tras e puxar a barra atras da nuca.',
     'Polia alta', 56),

    ('Remada curvada com barra', 'COSTAS', 'AVANCADO',
     'Tronco inclinado a cerca de 45 graus, coluna neutra, joelhos levemente flexionados. Puxe a barra em direcao ao umbigo e desca controlando.',
     'Extensoes de quadril sem carga e duas series leves do proprio movimento.',
     'Arredondar a lombar e usar impulso do tronco para subir a carga.', 'Barra', 54),

    ('Desenvolvimento militar', 'OMBRO', 'INTERMEDIARIO',
     'Em pe ou sentado, barra na altura das clavicula. Empurre acima da cabeca ate quase estender os cotovelos, mantendo o abdomen firme, e desca controlando.',
     'Rotacao externa de ombro com elastico, duas series de 15.',
     'Arquear a lombar para compensar falta de mobilidade de ombro.', 'Barra', 52),

    ('Elevacao lateral', 'OMBRO', 'INICIANTE',
     'Em pe, halteres ao lado do corpo. Eleve os bracos ate a altura dos ombros com leve flexao de cotovelo, e desca devagar.',
     'Uma serie sem carga para reconhecer o movimento.',
     'Usar carga alta e jogar o corpo, tirando o ombro do trabalho.', 'Halteres', 50),

    ('Rosca direta com barra', 'BICEPS', 'INICIANTE',
     'Em pe, pegada supinada na largura dos ombros. Flexione os cotovelos mantendo-os junto ao tronco e desca controlando ate estender.',
     'Duas series leves com a barra vazia.', 'Balancar o tronco e afastar os cotovelos do corpo.',
     'Barra', 48),

    ('Rosca martelo', 'BICEPS', 'INICIANTE',
     'Halteres com pegada neutra, palmas viradas uma para a outra. Flexione sem girar o punho e desca controlando.',
     'Uma serie leve de 15 repeticoes.', 'Girar o punho durante o movimento, virando em rosca alternada.',
     'Halteres', 46),

    ('Triceps na polia com corda', 'TRICEPS', 'INICIANTE',
     'Em pe de frente para a polia alta, cotovelos junto ao tronco. Estenda os cotovelos abrindo a corda no fim do movimento e volte controlando.',
     'Uma serie leve de 15 repeticoes na propria polia.',
     'Afastar os cotovelos do corpo e usar o ombro para empurrar.', 'Polia alta e corda', 44),

    ('Triceps testa', 'TRICEPS', 'INTERMEDIARIO',
     'Deitado no banco, barra acima da testa. Flexione os cotovelos levando a barra ate a altura da testa e estenda sem mover os bracos.',
     'Triceps na polia, duas series leves.', 'Abrir os cotovelos e mover o ombro junto.',
     'Barra W e banco reto', 42),

    ('Agachamento livre', 'PERNA', 'AVANCADO',
     'Barra apoiada no trapezio, pes na largura dos ombros. Desca empurrando o quadril para tras ate a coxa ficar ao menos paralela ao chao, e suba empurrando o chao.',
     'Mobilidade de tornozelo e quadril, mais duas series com a barra vazia.',
     'Joelho colapsando para dentro e calcanhar saindo do chao.', 'Barra e suporte', 40),

    ('Leg press 45', 'PERNA', 'INICIANTE',
     'Sentado no aparelho, pes na plataforma na largura dos ombros. Desca ate cerca de 90 graus de joelho sem tirar o quadril do apoio, e empurre sem travar os joelhos.',
     'Uma serie leve de 15 repeticoes no proprio aparelho.',
     'Descer demais e soltar a lombar do encosto.', 'Leg press', 38),

    ('Cadeira extensora', 'PERNA', 'INICIANTE',
     'Sentado, tornozelos atras do apoio. Estenda os joelhos ate quase travar e desca controlando.',
     'Uma serie sem carga.', 'Soltar a descida e bater o peso.', 'Cadeira extensora', 36),

    ('Elevacao pelvica', 'GLUTEO', 'INTERMEDIARIO',
     'Costas apoiadas no banco, barra sobre o quadril com protecao. Eleve o quadril ate alinhar tronco e coxa, aperte o gluteo no topo e desca controlando.',
     'Ponte de gluteo sem carga, duas series de 15.',
     'Hiperextender a lombar em vez de estender o quadril.', 'Barra, banco e colchonete', 34),

    ('Prancha isometrica', 'ABDOMEN', 'INICIANTE',
     'Apoio nos antebracos e nas pontas dos pes, corpo alinhado da cabeca aos calcanhares. Mantenha o abdomen contraido e respire normalmente.',
     'Nenhum aquecimento especifico; pode ser usado como aquecimento.',
     'Elevar o quadril ou deixar a lombar afundar.', 'Colchonete', 32)
) AS d(nome, grupo, nivel, execucao, aquecimento, erros, equipamento, dias)
ON CONFLICT (nome) DO NOTHING;
