# API

## Visao geral

- Base URL local: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html` (quando `APP_SWAGGER_ENABLED=true`)
- Healthcheck: `GET /actuator/health`
- Formato principal: JSON
- Autenticacao: Bearer JWT

## Autenticacao JWT

### Fluxo

1. Execute `POST /auth/login`
2. Receba um `token`
3. Envie o header:

```http
Authorization: Bearer <token>
```

### Exemplo de login

Request:

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@temperomineiro.com",
  "password": "123456"
}
```

Response:

```json
{
  "token": "<jwt>",
  "user": {
    "id": 1,
    "nome": "Administrador",
    "email": "admin@temperomineiro.com",
    "restaurante": "Tempero Mineiro - Demo",
    "restauranteId": 1,
    "roles": ["ADMIN", "GERENTE"]
  }
}
```

## Convencoes da API

### Paginacao

Endpoints paginados retornam a estrutura abaixo:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

### Erros

Falhas sao padronizadas pelo `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-03-24T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Credenciais invalidas.",
  "path": "/auth/login"
}
```

## Endpoints

### Autenticacao

| Metodo | Endpoint | Auth | Perfis | Descricao |
| --- | --- | --- | --- | --- |
| `POST` | `/auth/register` | Nao | Publico | Cadastra restaurante e usuario administrador (somente quando `APP_ALLOW_PUBLIC_REGISTER=true`) |
| `POST` | `/auth/login` | Nao | Publico | Realiza login e retorna JWT |

Exemplo de cadastro inicial:

```json
{
  "restaurantName": "Tempero Central",
  "restaurantSlug": "tempero-central",
  "adminName": "Admin Central",
  "adminEmail": "admin@temperocentral.com",
  "password": "Senha@2026"
}
```

### Mesas

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/mesas` | `ADMIN`, `GERENTE`, `GARCOM`, `CAIXA` | Lista mesas com paginacao |
| `POST` | `/mesas` | `ADMIN`, `GERENTE` | Cria mesa |
| `PUT` | `/mesas/{id}` | `ADMIN`, `GERENTE` | Atualiza dados da mesa |
| `PATCH` | `/mesas/{id}/status` | `ADMIN`, `GERENTE`, `GARCOM`, `CAIXA` | Atualiza status da mesa |
| `POST` | `/mesas/{id}/abrir` | `ADMIN`, `GERENTE`, `GARCOM` | Abre a mesa |
| `POST` | `/mesas/{id}/fechar` | `ADMIN`, `GERENTE`, `CAIXA` | Fecha a mesa manualmente |

Query params de `GET /mesas`:

- `search` opcional
- `page` padrao `0`
- `size` padrao `12`

Exemplo de criacao:

```json
{
  "nome": "Mesa 11",
  "capacidade": 4,
  "status": "LIVRE",
  "ativa": true
}
```

Exemplo de response:

```json
{
  "id": 11,
  "nome": "Mesa 11",
  "capacidade": 4,
  "status": "LIVRE",
  "publicToken": "8e0b0f9a3c9240f0a6cb2d8bb0e00abc",
  "qrCodeUrl": "/public/qr?text=http%3A%2F%2Flocalhost%3A8080%2Fpublic%2Ftempero-mineiro%2Fmenu%3FmesaToken%3D8e0b0f9a3c9240f0a6cb2d8bb0e00abc",
  "abertaEm": null,
  "ativa": true
}
```

Exemplo de troca de status:

```json
{
  "status": "OCUPADA"
}
```

### Categorias

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/categorias` | `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA`, `CAIXA` | Lista categorias com paginacao |
| `GET` | `/categorias/ativas` | `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA`, `CAIXA` | Lista apenas categorias ativas |
| `POST` | `/categorias` | `ADMIN`, `GERENTE` | Cria categoria |
| `PUT` | `/categorias/{id}` | `ADMIN`, `GERENTE` | Atualiza categoria |
| `DELETE` | `/categorias/{id}` | `ADMIN`, `GERENTE` | Desativa categoria |

Exemplo de request:

```json
{
  "nome": "Pratos",
  "descricao": "Pratos principais da casa",
  "ordemExibicao": 1,
  "ativa": true
}
```

Exemplo de response:

```json
{
  "id": 1,
  "nome": "Pratos",
  "descricao": "Pratos principais da casa",
  "ordemExibicao": 1,
  "ativa": true
}
```

### Produtos

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/produtos` | `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA`, `CAIXA` | Lista produtos com filtro opcional por categoria |
| `POST` | `/produtos` | `ADMIN`, `GERENTE` | Cria produto |
| `PUT` | `/produtos/{id}` | `ADMIN`, `GERENTE` | Atualiza produto |
| `DELETE` | `/produtos/{id}` | `ADMIN`, `GERENTE` | Marca produto como indisponivel |

Query params de `GET /produtos`:

- `search` opcional
- `categoriaId` opcional
- `page` padrao `0`
- `size` padrao `12`

Exemplo de request:

```json
{
  "categoriaId": 1,
  "nome": "Feijao Tropeiro Completo",
  "descricao": "Classico mineiro com torresmo e couve.",
  "preco": 39.90,
  "imagemUrl": "https://images.example.com/produtos/tropeiro.jpg",
  "disponivel": true,
  "receita": [
    {
      "estoqueId": 1,
      "quantidadeConsumida": 0.350
    }
  ]
}
```

Exemplo de response:

```json
{
  "id": 1,
  "categoriaId": 1,
  "categoriaNome": "Pratos",
  "nome": "Feijao Tropeiro Completo",
  "descricao": "Classico mineiro com torresmo e couve.",
  "preco": 39.90,
  "imagemUrl": "https://images.example.com/produtos/tropeiro.jpg",
  "disponivel": true,
  "sku": "TM-1-AB12CD34",
  "receita": [
    {
      "id": 1,
      "estoqueId": 1,
      "estoqueNome": "Feijao tropeiro",
      "quantidadeConsumida": 0.350
    }
  ]
}
```

### Pedidos

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/pedidos` | `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA`, `CAIXA` | Lista pedidos |
| `GET` | `/pedidos/{id}` | `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA`, `CAIXA` | Busca pedido por id |
| `POST` | `/pedidos` | `ADMIN`, `GERENTE`, `GARCOM` | Cria pedido interno |
| `POST` | `/pedidos/{id}/itens` | `ADMIN`, `GERENTE`, `GARCOM` | Adiciona item ao pedido |
| `DELETE` | `/pedidos/{id}/itens/{itemId}` | `ADMIN`, `GERENTE`, `GARCOM` | Remove item do pedido |
| `PATCH` | `/pedidos/{id}/status` | `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA` | Atualiza status do pedido |
| `POST` | `/pedidos/{id}/entrega` | `ADMIN`, `GERENTE`, `GARCOM`, `CAIXA` | Inicia fluxo inteligente de entrega (proprio/uber/automatico) |

Query params de `GET /pedidos`:

- `status` opcional
- `mesaId` opcional
- `page` padrao `0`
- `size` padrao `10`

Exemplo de criacao:

```json
{
  "mesaId": 1,
  "observacoes": "Sem cebola",
  "origem": "SALAO",
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 2,
      "observacoes": "Caprichar na couve"
    }
  ],
  "desconto": 0,
  "taxaServico": 2
}
```

Exemplo de response:

```json
{
  "id": 1,
  "mesaId": 1,
  "mesaNome": "Mesa 1",
  "usuarioId": 3,
  "usuarioNome": "Garcom",
  "status": "EM_PREPARO",
  "origem": "SALAO",
  "observacoes": "Sem cebola",
  "itens": [
    {
      "id": 1,
      "produtoId": 1,
      "produtoNome": "Feijao Tropeiro Completo",
      "quantidade": 2,
      "precoUnitario": 39.90,
      "total": 79.80,
      "observacoes": "Caprichar na couve"
    }
  ],
  "subtotal": 79.80,
  "desconto": 0.00,
  "taxaServico": 2.00,
  "total": 81.80,
  "abertoEm": "2026-03-24T15:00:00Z",
  "prontoEm": null,
  "entregueEm": null,
  "fechadoEm": null
}
```

Exemplo de atualizacao de status:

```json
{
  "status": "PRONTO"
}
```

### Cozinha

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/cozinha/pedidos` | `ADMIN`, `GERENTE`, `COZINHA` | Lista pedidos em preparo e prontos |
| `GET` | `/cozinha/stream` | `ADMIN`, `GERENTE`, `COZINHA` | Abre stream SSE da cozinha |

Exemplo de evento SSE:

```text
event: connected
data: {"type":"CONNECTED","message":"Canal de notificacoes ativo.","entityId":null,"timestamp":"2026-03-24T15:00:00Z"}
```

### Caixa

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/caixa/mesas/{mesaId}/resumo` | `ADMIN`, `GERENTE`, `CAIXA` | Retorna consumo, pagamentos e saldo da mesa |
| `POST` | `/caixa/mesas/{mesaId}/pagamentos` | `ADMIN`, `GERENTE`, `CAIXA` | Registra pagamento |
| `POST` | `/caixa/mesas/{mesaId}/fechar` | `ADMIN`, `GERENTE`, `CAIXA` | Fecha conta e conclui pedidos |

Exemplo de pagamento:

```json
{
  "metodo": "PIX",
  "valor": 81.80,
  "observacoes": "Pagamento integral"
}
```

Exemplo de response do pagamento:

```json
{
  "id": 1,
  "metodo": "PIX",
  "status": "CONCLUIDO",
  "valor": 81.80,
  "observacoes": "Pagamento integral",
  "pagoEm": "2026-03-24T15:10:00Z"
}
```

Exemplo de resumo:

```json
{
  "mesaId": 1,
  "mesaNome": "Mesa 1",
  "totalConsumido": 81.80,
  "totalPago": 40.00,
  "saldoPendente": 41.80,
  "pagamentos": [
    {
      "id": 1,
      "metodo": "PIX",
      "status": "CONCLUIDO",
      "valor": 40.00,
      "observacoes": "Pagamento parcial",
      "pagoEm": "2026-03-24T15:10:00Z"
    }
  ],
  "pedidos": [
    {
      "id": 1,
      "mesaId": 1,
      "mesaNome": "Mesa 1",
      "usuarioId": 3,
      "usuarioNome": "Garcom",
      "status": "ENTREGUE",
      "origem": "SALAO",
      "observacoes": "Sem cebola",
      "itens": [
        {
          "id": 1,
          "produtoId": 1,
          "produtoNome": "Feijao Tropeiro Completo",
          "quantidade": 2,
          "precoUnitario": 39.90,
          "total": 79.80,
          "observacoes": "Caprichar na couve"
        }
      ],
      "subtotal": 79.80,
      "desconto": 0.00,
      "taxaServico": 2.00,
      "total": 81.80,
      "abertoEm": "2026-03-24T15:00:00Z",
      "prontoEm": "2026-03-24T15:05:00Z",
      "entregueEm": "2026-03-24T15:07:00Z",
      "fechadoEm": null
    }
  ]
}
```

### Estoque

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/estoque` | `ADMIN`, `GERENTE` | Lista itens de estoque |
| `GET` | `/estoque/movimentacoes` | `ADMIN`, `GERENTE` | Lista movimentacoes |
| `GET` | `/estoque/baixo` | `ADMIN`, `GERENTE` | Lista estoque baixo |
| `POST` | `/estoque` | `ADMIN`, `GERENTE` | Cria item de estoque |
| `PUT` | `/estoque/{id}` | `ADMIN`, `GERENTE` | Atualiza item de estoque |
| `POST` | `/estoque/{id}/ajustes` | `ADMIN`, `GERENTE` | Executa ajuste manual |

Exemplo de cadastro:

```json
{
  "nome": "Feijao tropeiro",
  "unidadeMedida": "KG",
  "quantidadeAtual": 25.000,
  "quantidadeMinima": 5.000,
  "custoUnitario": 18.00,
  "ativo": true
}
```

Exemplo de ajuste:

```json
{
  "tipo": "ENTRADA",
  "quantidade": 3.000,
  "motivo": "Reposicao"
}
```

Exemplo de response:

```json
{
  "id": 1,
  "nome": "Feijao tropeiro",
  "unidadeMedida": "KG",
  "quantidadeAtual": 28.000,
  "quantidadeMinima": 5.000,
  "custoUnitario": 18.00,
  "ativo": true,
  "estoqueBaixo": false
}
```

### Relatorios

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/dashboard` | `ADMIN`, `GERENTE` | Indicadores operacionais resumidos |
| `GET` | `/relatorios/vendas` | `ADMIN`, `GERENTE` | Relatorio de vendas por periodo |

Query params de `GET /relatorios/vendas`:

- `startDate` opcional no formato `YYYY-MM-DD`
- `endDate` opcional no formato `YYYY-MM-DD`

Exemplo de response do dashboard:

```json
{
  "faturamentoDia": 150.00,
  "faturamentoMes": 4210.50,
  "pedidosAbertos": 3,
  "mesasOcupadas": 2,
  "alertasEstoqueBaixo": 1
}
```

Exemplo de response do relatorio:

```json
{
  "resumo": {
    "faturamento": 4210.50,
    "ticketMedio": 84.21
  },
  "produtosMaisVendidos": [
    {
      "nome": "Feijao Tropeiro Completo",
      "quantidade": 18,
      "valor": 718.20
    }
  ]
}
```

### Usuarios

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/users` | `ADMIN`, `GERENTE` | Lista usuarios |
| `POST` | `/users` | `ADMIN`, `GERENTE` | Cria usuario |
| `PUT` | `/users/{id}` | `ADMIN`, `GERENTE` | Atualiza usuario |

Exemplo de criacao:

```json
{
  "nome": "Novo Garcom",
  "email": "garcom2@temperomineiro.com",
  "password": "Senha@123",
  "roles": ["GARCOM"]
}
```

Exemplo de response:

```json
{
  "id": 6,
  "nome": "Novo Garcom",
  "email": "garcom2@temperomineiro.com",
  "restaurante": "Tempero Mineiro - Demo",
  "restauranteId": 1,
  "roles": ["GARCOM"]
}
```

### Publico

| Metodo | Endpoint | Auth | Descricao |
| --- | --- | --- | --- |
| `GET` | `/public/{restaurantSlug}/menu?mesaToken=...` | Nao | Retorna cardapio publico da mesa |
| `POST` | `/public/{restaurantSlug}/orders` | Nao | Cria pedido publico da mesa |
| `GET` | `/public/qr?text=...` | Nao | Gera QR Code PNG |

Exemplo de menu publico:

```json
{
  "restaurante": "Tempero Mineiro - Demo",
  "mesa": "Mesa 1",
  "mesaToken": "8e0b0f9a3c9240f0a6cb2d8bb0e00abc",
  "categorias": [
    {
      "id": 1,
      "nome": "Pratos",
      "descricao": "Pratos principais da casa",
      "produtos": [
        {
          "id": 1,
          "nome": "Feijao Tropeiro Completo",
          "descricao": "Classico mineiro com torresmo e couve.",
          "preco": 39.90,
          "imagemUrl": "https://images.example.com/produtos/tropeiro.jpg"
        }
      ]
    }
  ]
}
```

Exemplo de pedido publico:

```json
{
  "mesaToken": "8e0b0f9a3c9240f0a6cb2d8bb0e00abc",
  "observacoes": "Pedido feito pelo cliente",
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 1,
      "observacoes": "Sem pimenta"
    }
  ]
}
```

### Notificacoes

| Metodo | Endpoint | Perfis | Descricao |
| --- | --- | --- | --- |
| `GET` | `/notifications/stream` | `ADMIN`, `GERENTE`, `GARCOM`, `COZINHA`, `CAIXA` | Stream SSE geral do restaurante |

## Observacoes de negocio refletidas na API

- pedidos internos nascem com origem `SALAO`; pedidos publicos usam `CARDAPIO_DIGITAL`
- ao criar pedido, a mesa passa para `OCUPADA`
- o fechamento da conta exige que nao existam pedidos `EM_PREPARO` ou `PRONTO`
- o fechamento da conta exige saldo quitado
- categorias e produtos nao sao removidos fisicamente pelos endpoints de exclusao; o comportamento atual e desativacao logica
