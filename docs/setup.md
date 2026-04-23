# Setup

## Pre-requisitos

Para executar o projeto localmente, voce precisa de:

- Java 17
- Maven 3.9+ para execucao sem Docker
- Docker e Docker Compose para execucao containerizada
- PostgreSQL, caso nao use Docker para o banco

## Variaveis de ambiente

As configuracoes principais estao em `backend/src/main/resources/application.yml` e podem ser sobrescritas por variaveis de ambiente.

| Variavel                    | Obrigatoria                            | Exemplo                                            | Finalidade                              |
| --------------------------- | -------------------------------------- | -------------------------------------------------- | --------------------------------------- |
| `SPRING_PROFILES_ACTIVE`    | Sim                                    | `dev`                                              | Perfil ativo (`dev` ou `prod`)          |
| `POSTGRES_DB`               | Sim em Docker                          | `tempero_mineiro`                                  | Nome do banco no container PostgreSQL   |
| `POSTGRES_USER`             | Sim em Docker                          | `postgres`                                         | Usuario do banco no container           |
| `POSTGRES_PASSWORD`         | Sim em Docker                          | `troque-esta-senha`                                | Senha do banco no container             |
| `APP_JWT_SECRET`            | Sim                                    | `chave-com-32-bytes-ou-mais`                       | Assinatura do JWT                       |
| `APP_JWT_EXPIRATION_MS`     | Nao                                    | `86400000`                                         | Validade do token                       |
| `APP_PUBLIC_BASE_URL`       | Sim em uso publico                     | `http://localhost:8080`                            | Base usada em links e QR Codes          |
| `APP_CORS_ALLOWED_ORIGINS`  | Sim em ambientes com clientes externos | `http://localhost,http://localhost:8080`           | Origens permitidas                      |
| `APP_SEED_ENABLED`          | Nao                                    | `true` (dev) / `false` (prod)                      | Habilita seed demo                      |
| `APP_SWAGGER_ENABLED`       | Nao                                    | `true` (dev) / `false` (prod)                      | Habilita Swagger e OpenAPI              |
| `APP_ALLOW_PUBLIC_REGISTER` | Nao                                    | `true` (dev) / `false` (prod)                      | Permite `POST /auth/register` publico   |
| `SERVER_PORT`               | Nao                                    | `8080`                                             | Porta HTTP da API                       |

## Arquivos de apoio

- `.env.example`: exemplo de configuracao com hardening padrao
- `.env`: configuracao local atual
- `docker-compose.yml`: orquestracao de backend e PostgreSQL

## Opcao 1: rodar com Docker

### Passo a passo

1. Na raiz do repositorio, copie `.env.example` para `.env` e ajuste as variaveis:

```bash
cp .env.example .env
```

2. Confirme que `SPRING_PROFILES_ACTIVE=dev` no `.env`.
3. Rode:

```bash
docker compose up -d --build
```

4. Aguarde a inicializacao dos containers.
5. Valide a saude da API:

```bash
curl http://localhost:8080/actuator/health
```

6. Abra o Swagger:

```text
http://localhost:8080/swagger-ui.html
```

### Portas usadas no Compose

- `8080`: backend

Observacao: o PostgreSQL nao fica publicado na porta do host por padrao.

### Comandos uteis

Parar os containers:

```bash
docker compose down
```

Parar e remover orfaos:

```bash
docker compose down --remove-orphans
```

Subir novamente com rebuild:

```bash
docker compose up -d --build
```

## Opcao 2: rodar sem Docker

### Banco de dados

Crie ou disponibilize um PostgreSQL local. O `application.yml` usa por padrao:

```text
jdbc:postgresql://localhost:5432/tempero_mineiro_erp
```

Se preferir outro nome de banco, sobrescreva `SPRING_DATASOURCE_URL`.

### Execucao

No diretorio `backend`:

```bash
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

## Primeiro acesso

Com a aplicacao no ar:

1. Abra `http://localhost:8080/swagger-ui.html`
2. Execute `POST /auth/login`
3. Use as credenciais demo, se o seed estiver ativo:

```json
{
  "email": "admin@temperomineiro.com",
  "password": "123456"
}
```

4. Copie o campo `token`
5. Clique em `Authorize`
6. Informe:

```text
Bearer SEU_TOKEN
```

## Seed de dados

Quando `APP_SEED_ENABLED=true`, o `DataSeeder` cria:

- restaurante demo
- roles padrao
- usuarios demo
- mesas demo
- categorias demo
- produtos demo
- itens de estoque demo

## Observacoes importantes de configuracao

- `APP_PUBLIC_BASE_URL` define a URL usada para montar o link de cardapio publico e o QR Code das mesas
- o CORS padrao permite `http://localhost` e `http://localhost:8080`
- em `prod`, `APP_SEED_ENABLED`, `APP_SWAGGER_ENABLED` e `APP_ALLOW_PUBLIC_REGISTER` devem ficar `false`
- o projeto usa `ddl-auto=update`, entao o schema e gerado/atualizado automaticamente
- em testes automatizados, o seed e desativado

## Troubleshooting basico

### Swagger nao abre

Verifique:

- se o backend esta rodando na porta `8080`
- se `GET /actuator/health` responde `UP`
- se voce esta acessando `http://localhost:8080/swagger-ui.html`

### Login demo nao funciona

Verifique:

- se `APP_SEED_ENABLED=true`
- se `SPRING_PROFILES_ACTIVE=dev`
- se o banco nao contem dados anteriores conflitantes
- se o ambiente atual esta apontando para o banco correto

### QR Code aponta para URL errada

Revise:

- `APP_PUBLIC_BASE_URL`

Em ambiente publico, essa variavel deve usar o dominio ou IP publico real da API.
