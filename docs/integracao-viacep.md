# Integração com a ViaCEP — documentação técnica

Projeto ForçaMente — PFC, Bacharelado em Sistemas de Informação, UMC, 2026.

Este documento descreve **como o ForçaMente consome uma API externa**: a decisão,
o desenho, o contrato exposto ao front, o tratamento de falhas e os testes que
sustentam tudo isso.

A referência da API externa em si — endpoints, formatos e contratos da ViaCEP —
está separada, em `src/main/resources/static/viacep/viacep-openapi.yaml`, e pode
ser lida renderizada conforme a seção [Visualizando a API externa](#visualizando-a-api-externa).

---

## 1. Por que uma API externa, e por que a ViaCEP

O cadastro de **professor** exige endereço completo. Pedir ao usuário que digite
logradouro, bairro, cidade e estado à mão traz dois problemas: é trabalhoso no
formulário e produz dados inconsistentes (abreviações, erros de grafia, cidade
que não pertence ao estado informado).

A ViaCEP resolve os dois: o usuário informa apenas o CEP e o restante do endereço
vem preenchido e padronizado, a partir da base dos Correios.

Foi escolhida por três razões objetivas:

- **Gratuita e sem autenticação** — não exige cadastro, chave de API nem
  contrato. Um projeto acadêmico não precisa provisionar credenciais de terceiro.
- **Escopo exato do problema** — faz uma coisa só, e é a coisa que precisamos.
- **Permite origem cruzada** — responde com `Access-Control-Allow-Origin: *`,
  o que mantinha aberta a alternativa de chamar direto do navegador.

Mesmo assim, **optamos por chamar pelo back-end** e não pelo front. As razões
estão na seção 3.

## 2. Onde a integração é usada

```
Tela de cadastro (front)
   usuário digita o CEP
        │
        ▼
GET /api/enderecos/{cep}          ← nossa API (rota pública)
        │
        ▼
GET https://viacep.com.br/ws/{cep}/json/   ← API externa
```

O endereço devolvido preenche os campos do formulário. Os campos **número** e
**complemento** continuam sendo digitados pelo usuário — a ViaCEP não os fornece,
porque não são função do CEP.

## 3. Desenho da integração

Quatro classes, uma responsabilidade cada:

| Camada | Arquivo | Responsabilidade |
|---|---|---|
| Controller | `controller/EnderecoController.java` | Expõe `GET /api/enderecos/{cep}` |
| Service | `service/impl/EnderecoService.java` | Valida, orquestra, traduz para o nosso contrato |
| Client | `integracao/ViaCepClient.java` | Fala HTTP com a ViaCEP; isola a falha de rede |
| Config | `config/ViacCepConfig.java` | Monta o `RestClient` com URL e tempos limite |

### Por que passar pelo back-end

Chamar a ViaCEP direto do front seria possível (ela permite CORS) e pouparia um
salto de rede. Preferimos o back-end porque:

- **A tradução de anomalias fica em um lugar só.** A ViaCEP tem comportamentos
  que contrariam REST (seção 6). Tratá-los no back significa que qualquer cliente
  nosso — este front, um app futuro, um script — recebe um contrato correto sem
  reimplementar as gambiarras.
- **A URL do terceiro fica configurável**, e não recompilada dentro do front.
- **O front não depende da disponibilidade do terceiro para saber o que exibir**:
  recebe um código HTTP nosso, já normalizado.

### Rota pública, por necessidade

`GET /api/enderecos/{cep}` é liberada sem token em `config/SegurancaConfig.java`.
Isso não é descuido: a consulta acontece **durante o cadastro**, antes de o
usuário ter conta e, portanto, antes de existir token. Uma rota autenticada
tornaria o autopreenchimento impossível.

O `EnderecoController` declara `@CrossOrigin` para `http://localhost:5173` e
`http://localhost:3000`, as origens de desenvolvimento do front.

## 4. Configuração

Em `src/main/resources/application.yaml`:

```yaml
viacep:
  url: ${VIACEP_URL:https://viacep.com.br/ws}
```

A URL é uma propriedade com valor padrão, não uma constante no código. Isso
permite apontar para um duplo de teste sem recompilar.

Os tempos limite estão em `ViacCepConfig`:

| Parâmetro | Valor | Por quê |
|---|---|---|
| `connectTimeout` | 3 s | Um terceiro fora do ar não pode travar o nosso cadastro |
| `readTimeout` | 3 s | Idem para resposta lenta |

Três segundos é um compromisso: generoso o bastante para uma rede ruim, curto o
bastante para o usuário não achar que o formulário congelou. **Sem tempo limite
explícito, o padrão é esperar indefinidamente** — é o erro clássico de integração
com terceiros, e a razão de esse `@Bean` existir em vez de um `RestClient` cru.

## 5. Contrato exposto ao front

### Requisição

```
GET /api/enderecos/{cep}
```

O `{cep}` é normalizado antes do uso: `EnderecoService` remove tudo que não é
dígito (`replaceAll("\\D", "")`), então `08780-000`, `08780 000` e `08780000`
são equivalentes. Depois da limpeza, exige **exatamente 8 dígitos**.

### Resposta 200

```json
{
  "cep": "08780000",
  "logradouro": "Avenida Vereador Narciso Yague Guimarães",
  "bairro": "Centro Cívico",
  "cidade": "Mogi das Cruzes",
  "estado": "SP"
}
```

### Respostas de erro

Todas usam o corpo padrão da API, `ApiErroDTO(status, message, campos)`:

| Código | Quando | Mensagem |
|---|---|---|
| `400` | CEP sem 8 dígitos após normalização | `O CEP deve conter 8 dígitos` |
| `404` | CEP bem formado, ausente da base | `Endereço não encontrado para o CEP: {cep}` |
| `503` | ViaCEP fora do ar, lenta ou recusando | `Serviço de consulta de CEP indisponível no momento, tente mais tarde` |

## 6. A parte que importa: traduzir as anomalias do terceiro

A ViaCEP se comporta de maneiras que quebrariam um cliente ingênuo. O papel da
integração é **absorver** isso, e não repassar.

| Comportamento da ViaCEP | O que devolvemos | Onde |
|---|---|---|
| CEP inexistente → **HTTP 200** com `{"erro": "true"}` | **404** | `EnderecoService:31` |
| Valor de `erro` é a *string* `"true"`, não booleano | Convertido para `Boolean` na desserialização | `ViaCepResponseDTO` |
| CEP malformado → **400 com corpo HTML** | Nunca acontece: barramos antes de sair na rede | `EnderecoService:24` |
| Qualquer falha de rede ou status de erro | **503** com mensagem única | `ViaCepClient:22` |
| Devolve o CEP formatado (`08780-000`) | Devolvemos só dígitos (`08780000`) | `EnderecoService:36` |
| Devolve 13 campos | Devolvemos 5 | `EnderecoResponseDTO` |

O primeiro item é o mais importante do documento. **Um CEP que não existe devolve
200 na ViaCEP.** Um cliente que confie apenas no código de status concluiria que
encontrou o endereço, e seguiria com um objeto de campos vazios. Nós inspecionamos
o corpo e devolvemos 404 — o código que o caso realmente significa.

O penúltimo item é uma decisão de consistência: o resto da nossa API trata CEP
como 8 dígitos, inclusive a coluna `cep` da tabela `usuarios`, que é
`varchar(8)`. Devolver com hífen obrigaria o front a limpar antes de enviar no
cadastro.

### Mapeamento de campos

Dos 13 campos da ViaCEP, consumimos 5 — e renomeamos 2 para o vocabulário da
nossa API:

| ViaCEP | Nosso | Observação |
|---|---|---|
| `cep` | `cep` | Reescrito sem hífen |
| `logradouro` | `logradouro` | — |
| `bairro` | `bairro` | — |
| `localidade` | `cidade` | Renomeado |
| `uf` | `estado` | Renomeado |
| `erro` | — | Consumido internamente, virou o 404 |
| `complemento`, `unidade`, `estado`, `regiao`, `ibge`, `gia`, `ddd`, `siafi` | — | Descartados: nenhum requisito os usa |

Descartar oito campos é deliberado. Trazê-los "porque vêm de graça" ampliaria o
contrato com dados que ninguém pediu — e, sob a LGPD, dado que não tem
finalidade declarada não deveria ser coletado.

## 7. CEPs gerais: o caso que não é erro

Municípios pequenos têm CEP único para a cidade inteira. A ViaCEP responde 200,
porém com `logradouro` e `bairro` como **strings vazias** — nunca nulas.

Decisão registrada: **devolvemos o endereço incompleto como 200**, e a tela
completa os campos que vieram vazios. A alternativa — tratar como 404 — seria
errada, porque o CEP existe.

Exemplo real, CEP `28300-000` (Itaperuna, RJ): vem `cidade` e `estado`
preenchidos, `logradouro` e `bairro` vazios.

## 8. O que acontece se a ViaCEP cair

**O cadastro não fica bloqueado no back-end.** Vale registrar isso com clareza,
porque é uma pergunta natural de quem lê o desenho.

`UsuarioService.criarUsuario` **não chama a ViaCEP**. A validação do endereço no
cadastro é apenas de formato (`cep` casando com `\d{8}`, em `UsuarioRequestDTO`).
A ViaCEP é usada só pelo autopreenchimento, num endpoint separado. Se ela estiver
fora, `GET /api/enderecos/{cep}` devolve 503, mas
`POST /api/usuarios` continua aceitando um endereço digitado à mão.

Ou seja: a indisponibilidade do terceiro degrada a **conveniência**, não a
**funcionalidade**. Isso é resultado do desenho — o terceiro não está no caminho
crítico.

> **Decisão pendente, do front:** ao receber 503, a tela deve liberar os campos de
> endereço para digitação manual, ou impedir o cadastro? O back-end suporta as
> duas. Ainda não foi decidido com o Felipe.

## 9. Testes

`src/test/java/forcamente/api/service/EnderecoServiceTest.java` cobre cinco casos,
com a ViaCEP substituída por dublê — nenhum teste depende de rede:

| Teste | O que prova |
|---|---|
| `deveDevolverEnderecoMapeado` | O mapeamento e a renomeação de campos |
| `deveDevolverCepGeralIncompleto` | CEP geral volta 200 incompleto, não 404 |
| `deveRejeitarCepInexistente` | 200 + `erro` da ViaCEP vira 404 nosso |
| `deveRejeitarCepMalFormadoSemChamarOViaCep` | CEP inválido não gera chamada externa |
| `devePropagarServicoIndisponivel` | Falha do terceiro chega como 503 |

O quarto merece destaque: ele verifica não só o código de resposta, mas que a
chamada externa **não aconteceu**. Validar antes de sair na rede evita tráfego
inútil e um 400 com corpo HTML que teríamos de tratar.

## Visualizando a API externa

A ViaCEP não publica especificação OpenAPI oficial. Escrevemos uma, a partir do
comportamento observado e verificado por requisições reais em 27/09/2026.

Com o back-end no ar:

```
http://localhost:8080/viacep/index.html
```

O `index.html` no fim é necessário: o Spring resolve página inicial
automaticamente apenas na raiz da aplicação, não em subpastas de recursos
estáticos. `http://localhost:8080/viacep/` responde 404.

A página é um Swagger UI que carrega apenas o documento da ViaCEP — a nossa API
não é exposta ali. O botão **Try it out** funciona e faz chamadas reais ao
serviço, porque a ViaCEP permite origem cruzada.

Arquivos: `src/main/resources/static/viacep/index.html` e
`viacep-openapi.yaml`, no mesmo diretório.

## Pontos em aberto

- **Comportamento do front no 503** (seção 8) — decisão do Felipe.
- **`EnderecoService` registra o CEP no log** (`log.info("buscarPorCep: {}")`).
  Um CEP isolado não identifica alguém, mas correlacionado a outros registros
  pode contribuir para isso. Vale decidir se o log permanece.
- **Limites de uso da ViaCEP não foram medidos.** O serviço não os publica. Em
  volume de produção, convém medir antes de confiar.
- **`ViacCepConfig` tem um "c" a mais no nome** da classe. Cosmético; renomear
  exige apenas ajustar o arquivo, já que o `@Bean` é localizado por tipo.
