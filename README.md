# Tempero Mineiro ERP

[![Backend CI](https://github.com/msouza34/tempero-mineiro/actions/workflows/backend-ci.yml/badge.svg?branch=main)](https://github.com/msouza34/tempero-mineiro/actions/workflows/backend-ci.yml)

Backend para ERP de bares e restaurantes com Spring Boot, PostgreSQL e Docker, com fluxo de entrega inteligente (proprio, Uber ou automatico).

## Tecnologias

- Java 17
- Spring Boot
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Docker e Docker Compose
- Swagger / OpenAPI

## Modulos principais

- Autenticacao e cadastro de restaurante
- Gestao de mesas
- Cardapio, categorias e produtos
- Pedidos
- Entrega inteligente com fallback
- Cozinha com SSE
- Caixa e pagamentos
- Estoque
- Relatorios
- Usuarios e perfis
- Endpoints publicos para menu e QR Code

## Modos de execucao

- `dev`: seed, Swagger e cadastro publico habilitados
- `prod`: seed, Swagger e cadastro publico desabilitados por padrao

## Como rodar com Docker (dev)

1. Copie o arquivo de exemplo e ajuste os valores:

```bash
cp .env.example .env
```

2. Suba os servicos:

```bash
docker compose up -d --build
```

Servicos:

- Backend API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- PostgreSQL: exposto apenas na rede interna do Docker (nao publicado na maquina host)

## Como rodar em desenvolvimento

```bash
cd backend
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

## Primeiro acesso no Swagger (dev)

1. Abra `http://localhost:8080/swagger-ui.html`
2. No topo da documentacao, abra o grupo `Autenticacao`
3. Execute `POST /auth/login` com:

```json
{
  "email": "admin@temperomineiro.com",
  "password": "123456"
}
```

4. Copie o campo `token` da resposta
5. Clique em `Authorize`
6. Cole:

```text
Bearer SEU_TOKEN
```

Depois disso, os endpoints protegidos ficam liberados no Swagger.

## Credenciais seed (somente dev)

Quando `APP_SEED_ENABLED=true`, o sistema cria:

- `admin@temperomineiro.com` / `123456`
- `gerente@temperomineiro.com` / `123456`
- `garcom@temperomineiro.com` / `123456`
- `cozinha@temperomineiro.com` / `123456`
- `caixa@temperomineiro.com` / `123456`

## Variaveis importantes

- `SPRING_PROFILES_ACTIVE` (`dev` ou `prod`)
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `APP_JWT_SECRET`
- `APP_PUBLIC_BASE_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_SEED_ENABLED`
- `APP_SWAGGER_ENABLED`
- `APP_ALLOW_PUBLIC_REGISTER`

`APP_PUBLIC_BASE_URL` define a base usada para gerar links e QR Codes das mesas. Em backend-only, os QR Codes apontam para:

`/public/{restaurantSlug}/menu?mesaToken=...`

## Endpoints principais

### Autenticacao

- `POST /auth/login`
- `POST /auth/register` (somente quando `APP_ALLOW_PUBLIC_REGISTER=true`)

### Operacao

- `GET|POST|PUT /mesas`
- `PATCH /mesas/{id}/status`
- `POST /mesas/{id}/abrir`
- `POST /mesas/{id}/fechar`
- `GET|POST|PUT /categorias`
- `GET /categorias/ativas`
- `GET|POST|PUT /produtos`
- `GET|POST /pedidos`
- `GET /pedidos/{id}`
- `POST /pedidos/{id}/itens`
- `DELETE /pedidos/{id}/itens/{itemId}`
- `PATCH /pedidos/{id}/status`
- `POST /pedidos/{id}/entrega`
- `GET /cozinha/pedidos`
- `GET /cozinha/stream`
- `GET /caixa/mesas/{mesaId}/resumo`
- `POST /caixa/mesas/{mesaId}/pagamentos`
- `POST /caixa/mesas/{mesaId}/fechar`
- `GET|POST|PUT /estoque`
- `GET /estoque/movimentacoes`
- `GET /estoque/baixo`
- `POST /estoque/{id}/ajustes`
- `GET /dashboard`
- `GET /relatorios/vendas`
- `GET|POST|PUT /users`
- `GET /notifications/stream`

### Publico

- `GET /public/{restaurantSlug}/menu?mesaToken=...`
- `POST /public/{restaurantSlug}/orders`
- `GET /public/qr?text=...`

## Entrega inteligente

Endpoint principal:

- `POST /pedidos/{id}/entrega`

Regras implementadas:

1. Anti conflito: so permite iniciar quando `statusEntrega=AGUARDANDO`.
2. Pagamento:
- `DINHEIRO` forca `PROPRIO`.
- `ONLINE` permite `PROPRIO`, `UBER` ou `AUTOMATICO`.
3. Modo automatico:
- Se existe entregador disponivel e distancia `< 5km`, usa `PROPRIO`.
- Se nao tiver entregador ou estiver em horario de pico, usa `UBER`.
4. Fallback:
- Se `PROPRIO` falhar (sem aceite em ate 2 minutos simulados), chama `UBER` automaticamente.
5. Diferencial anti duplicidade:
- Se ja existir `uberDeliveryId`, bloqueia nova chamada.

Enums de entrega:

- `TipoEntrega`: `PROPRIO`, `UBER`, `AUTOMATICO`
- `FormaPagamento`: `DINHEIRO`, `ONLINE`
- `StatusEntrega`: `AGUARDANDO`, `EM_PROCESSAMENTO`, `ENVIADO`, `ENTREGUE`, `ERRO`

Request de exemplo:

```json
{
  "tipoEntrega": "AUTOMATICO",
  "formaPagamento": "ONLINE",
  "distanciaEntrega": 3.2
}
```

Response de exemplo:

```json
{
  "pedidoId": 101,
  "tipoEntregaSolicitada": "AUTOMATICO",
  "tipoEntregaExecutada": "PROPRIO",
  "formaPagamento": "ONLINE",
  "statusEntrega": "ENTREGUE",
  "distanciaEntrega": 3.2,
  "uberDeliveryId": null,
  "fallbackAcionado": false,
  "mensagem": "Entrega iniciada com sucesso."
}
```

Observacao:

- No fluxo atual, apos enviar para o provedor de entrega, o servico finaliza o pedido de entrega no mesmo processamento e retorna `ENTREGUE`.

## Testes

```bash
cd backend
mvn test
```

## Build do backend

```bash
cd backend
mvn -DskipTests package
```

## Fluxo rapido de teste

Depois de fazer login no Swagger e autorizar com o token:

1. Liste os dados demo:
- `GET /mesas`
- `GET /categorias`
- `GET /produtos`
- `GET /estoque`

2. Crie uma mesa nova:
- `POST /mesas`

```json
{
  "nome": "Mesa Teste Swagger",
  "capacidade": 4,
  "status": "LIVRE",
  "ativa": true
}
```

3. Crie um item de estoque:
- `POST /estoque`

```json
{
  "nome": "Ingrediente Swagger",
  "unidadeMedida": "KG",
  "quantidadeAtual": 10,
  "quantidadeMinima": 2,
  "custoUnitario": 12,
  "ativo": true
}
```

4. Crie uma categoria:
- `POST /categorias`

```json
{
  "nome": "Categoria Swagger",
  "descricao": "Teste manual",
  "ordemExibicao": 10,
  "ativa": true
}
```

5. Crie um produto usando os ids da categoria e do estoque:
- `POST /produtos`

```json
{
  "categoriaId": 1,
  "nome": "Produto Swagger",
  "descricao": "Teste completo",
  "preco": 25,
  "imagemUrl": null,
  "disponivel": true,
  "receita": [
    {
      "estoqueId": 1,
      "quantidadeConsumida": 0.5
    }
  ]
}
```

6. Crie um pedido:
- `POST /pedidos`

```json
{
  "mesaId": 1,
  "observacoes": "Pedido via Swagger",
  "origem": "SALAO",
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 2,
      "observacoes": "Caprichar"
    }
  ],
  "desconto": 0,
  "taxaServico": 2
}
```

7. Passe o pedido para pronto:
- `PATCH /pedidos/{id}/status`

```json
{
  "status": "PRONTO"
}
```

8. Marque como entregue:
- `PATCH /pedidos/{id}/status`

```json
{
  "status": "ENTREGUE"
}
```

9. Veja o resumo da mesa:
- `GET /caixa/mesas/{mesaId}/resumo`

10. Registre o pagamento:
- `POST /caixa/mesas/{mesaId}/pagamentos`

```json
{
  "metodo": "PIX",
  "valor": 52,
  "observacoes": "Pagamento total"
}
```

11. Feche a conta:
- `POST /caixa/mesas/{mesaId}/fechar`

12. Confira o resultado:
- `GET /dashboard`
- `GET /relatorios/vendas`
