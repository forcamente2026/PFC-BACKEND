# ForçaMente — Backend

Documentação das tecnologias planejadas para o back-end da plataforma ForçaMente, conforme a Ficha de Caracterização do Projeto e a Monografia do PFC.

Projeto Final de Curso (PFC) — Bacharelado em Sistemas de Informação, Universidade de Mogi das Cruzes (UMC), 2026.

Repositório do front-end: [PFC-FRONTEND](https://github.com/ofelpys/PFC-FRONTEND)

> ### Entrega — consumo de API externa
>
> A documentação técnica da integração está em **[docs/integracao-viacep.md](docs/integracao-viacep.md)**.
>
> O documento cobre a API consumida ([ViaCEP](https://viacep.com.br/)), a justificativa da escolha, o desenho
> da integração, o contrato exposto ao front-end, o tratamento de falhas e os testes que o sustentam.
> A especificação OpenAPI da API externa acompanha a entrega e pode ser consultada, com o back-end
> no ar, em `http://localhost:8080/viacep/index.html`.

## Arquitetura

Modelo **cliente-servidor**: o front-end (React, publicado na Vercel) envia requisições HTTPS para o back-end via **API REST** protegida por autenticação **JWT**, com troca de dados em **JSON**. O back-end é estruturado como um **monólito modular**, organizado em cinco módulos principais para facilitar manutenção e evolução independente de cada domínio. A persistência dos dados é feita em um banco **PostgreSQL** hospedado no serviço **Neon**, via conexão **TLS/JDBC**.

## Tecnologias utilizadas

| Categoria | Tecnologia | Uso no projeto |
|---|---|---|
| Linguagem | [Java](https://www.java.com/) | Linguagem principal do back-end |
| Framework | [Spring Boot](https://spring.io/projects/spring-boot) | Framework de aplicação, arquitetura em camadas |
| Persistência | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) + [Hibernate](https://hibernate.org/) | Mapeamento objeto-relacional (ORM); Hibernate traduz entidades em tabelas sem uso direto de SQL, Spring Data JPA adiciona repositórios com métodos de acesso a dados prontos e suporte a consultas customizadas |
| Banco de dados | [PostgreSQL](https://www.postgresql.org/) (hospedado no [Neon](https://neon.tech/)) | SGBD relacional, escolhido pela conformidade ACID e por armazenar dados sensíveis do usuário com integridade |
| Autenticação/Sessão | [JWT (JSON Web Token)](https://jwt.io/) | Controle de sessão sem estado armazenado no servidor; o token é enviado pelo cliente a cada requisição |
| Segurança | [Spring Security](https://spring.io/projects/spring-security) | Validação do token JWT, autorização por papel (Aluno, Professor, Administrador) e controle de acesso aos recursos |
| Hash de senhas | [BCrypt](https://en.wikipedia.org/wiki/Bcrypt) | Hash de senhas e dados sensíveis antes da persistência no banco |
| API REST | Spring Web (`spring-boot-starter-web`) | Exposição de endpoints organizados por recurso, retorno em JSON, uso de DTOs para separar a entidade de persistência da camada de exposição, tratamento de exceções centralizado |
| API externa | [ViaCEP](https://viacep.com.br/) | Preenchimento automático de endereço durante o cadastro do usuário |
| Testes | [JUnit](https://junit.org/) + [Mockito](https://site.mockito.org/) | Testes unitários de regras de negócio (cadastro de aluno, criação de treino, etc.), sem dependência do React, Spring ou PostgreSQL |
| Documentação de API | [Swagger](https://swagger.io/) | Documentação e testes dos endpoints da API |
| Testes manuais de API | [Postman](https://www.postman.com/) | Testes manuais de requisições durante o desenvolvimento |
| Modelagem | [draw.io](https://app.diagrams.net/) | Diagramas de arquitetura e classes |
| Deploy / hospedagem | [Azure App Service](https://azure.microsoft.com/products/app-service) | Publicação do back-end (Spring Boot) |
| Versionamento | Git + GitHub | Controle de versão, branches e pull requests para revisão de código |

## Segurança e LGPD

- Autenticação via login e senha com token **JWT** para controle de sessão, com expiração configurada
- Senhas e dados sensíveis (CPF, histórico de saúde) armazenados com hash **BCrypt**, nunca em texto puro
- Autorização por perfil de usuário (roles) via **Spring Security**: Aluno, Professor e Administrador
- Comunicação via **HTTPS** e proteção contra vulnerabilidades comuns de API (injeção SQL, XSS)
- Tratamento de dados pessoais conforme a **LGPD**: consentimento no cadastro, possibilidade de exclusão/anonimização de dados mediante solicitação do titular, e registro de auditoria para acessos a dados sensíveis

## Módulos principais previstos

1. Gestão de Conta (login, autenticação e criptografia de usuários)
2. Gestão de Perfis — Multiusuário (Aluno, Professor, Administrador)
3. Conteúdo Educacional (gifs, guias, quizzes e documentação)
4. Acompanhamento de Desempenho (dashboard, calculadoras de volume de treino e TMB)
5. Segurança e Prevenção (Treino) / Segurança e Compliance (LGPD)
