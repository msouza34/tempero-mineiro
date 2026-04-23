# Tempero Mineiro ERP Docs

## Visao geral

O Tempero Mineiro ERP e um backend REST para operacao de bares e restaurantes. O sistema concentra autenticacao, gestao de mesas, cardapio, pedidos, cozinha, caixa, estoque, relatorios, usuarios e endpoints publicos para cardapio digital e QR Code.

O projeto esta em modo backend-only. Em ambiente `dev`, a interface operacional principal e a documentacao OpenAPI no Swagger (`http://localhost:8080/swagger-ui.html`).

## Objetivo do sistema

O objetivo do sistema e oferecer uma base unica para a rotina operacional de um restaurante, com suporte a:

- cadastro inicial do restaurante e do usuario administrador
- controle de acesso por perfil
- operacao de salao e cozinha
- fechamento de conta e baixa de estoque
- consulta publica do cardapio por mesa
- indicadores operacionais e relatorios de vendas

## Tecnologias utilizadas

| Camada              | Tecnologias                                         |
| ------------------- | --------------------------------------------------- |
| Linguagem e runtime | Java 17                                             |
| Framework principal | Spring Boot 3.3.5                                   |
| API                 | Spring Web, Spring Validation, Springdoc OpenAPI    |
| Seguranca           | Spring Security, JWT, BCrypt                        |
| Persistencia        | Spring Data JPA, Hibernate                          |
| Banco de dados      | PostgreSQL                                          |
| Testes              | JUnit 5, Spring Boot Test, Spring Security Test, H2 |
| Tempo real          | Server-Sent Events (SSE)                            |
| Infraestrutura      | Docker, Docker Compose                              |
| QR Code             | ZXing                                               |

## Principais funcionalidades

- Autenticacao com JWT
- Cadastro de restaurante com provisionamento do usuario administrador
- Gestao de usuarios e perfis `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA` e `CAIXA`
- Cadastro e operacao de mesas
- Cadastro de categorias, produtos e ficha tecnica
- Criacao e acompanhamento de pedidos
- Fila da cozinha e notificacoes via SSE
- Registro de pagamentos e fechamento de conta
- Controle de estoque com movimentacoes
- Cardapio publico por mesa com QR Code
- Dashboard e relatorio de vendas

## Estrutura desta documentacao

- [architecture.md](./architecture.md): arquitetura, camadas e fluxos
- [api.md](./api.md): endpoints, autenticacao e exemplos de payload
- [security.md](./security.md): mecanismos de seguranca e limites atuais
- [database.md](./database.md): modelo de dados, entidades e relacionamentos
- [setup.md](./setup.md): execucao local e configuracao
- [deployment.md](./deployment.md): publicacao em producao com Docker
- [testing.md](./testing.md): estrategia de testes e comandos
- [decisions.md](./decisions.md): decisoes tecnicas do projeto

## Como rodar localmente

### Opcao 1: Docker Compose

Na raiz do repositorio, copie `.env.example` para `.env` e rode:

```bash
docker compose up -d --build
```

Servicos esperados:

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Healthcheck: `http://localhost:8080/actuator/health`
- PostgreSQL: apenas rede interna do Docker (nao publicado no host)

### Opcao 2: Maven

1. Suba um PostgreSQL local.
2. Ajuste as variaveis de ambiente ou o arquivo `.env`.
3. Rode o backend:

```bash
cd backend
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

## Credenciais de seed

Quando `APP_SEED_ENABLED=true` (tipicamente em `dev`), o sistema cria um restaurante demo e os seguintes usuarios:

- `admin@temperomineiro.com` / `123456`
- `gerente@temperomineiro.com` / `123456`
- `garcom@temperomineiro.com` / `123456`
- `cozinha@temperomineiro.com` / `123456`
- `caixa@temperomineiro.com` / `123456`

## Primeiro acesso pelo Swagger

1. Abra `http://localhost:8080/swagger-ui.html`
2. Execute `POST /auth/login`
3. Copie o valor de `token`
4. Clique em `Authorize`
5. Informe `Bearer SEU_TOKEN`

Exemplo de login:

```json
{
  "email": "admin@temperomineiro.com",
  "password": "123456"
}
```

## Escopo atual

- O repositorio fornece backend, banco e documentacao OpenAPI.
- O projeto nao inclui frontend web ativo neste momento.
- O deploy suportado diretamente pelo repositorio e baseado em Docker para backend e PostgreSQL.
