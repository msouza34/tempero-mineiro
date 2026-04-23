# Deployment

## Visao geral

O repositorio oferece suporte direto a deploy do backend e do PostgreSQL com Docker. O caminho mais simples de producao e usar uma VPS ou servidor Linux com Docker e Docker Compose.

O projeto nao inclui configuracao versionada de Nginx, mas o uso de um proxy reverso na frente da API e recomendado para HTTPS, roteamento e controle adicional de acesso.

## Topologia recomendada

```text
Internet
   |
   v
Nginx ou proxy reverso
   |
   v
Spring Boot API :8080
   |
   v
PostgreSQL
```

## Componentes fornecidos pelo repositorio

- `backend/Dockerfile`: build e runtime da API
- `docker-compose.yml`: backend + PostgreSQL

## Build do backend

O Dockerfile usa duas etapas:

1. build com `maven:3.9.9-eclipse-temurin-17`
2. runtime com `eclipse-temurin:17-jre`

Comando equivalente de build local:

```bash
cd backend
mvn -DskipTests package
```

## Deploy com Docker Compose

### Passos

1. Copie o projeto para o servidor.
2. Ajuste as variaveis de ambiente.
3. Rode na raiz:

```bash
docker compose up -d --build
```

4. Valide o healthcheck:

```bash
curl http://localhost:8080/actuator/health
```

## Variaveis recomendadas para producao

| Variavel | Recomendacao |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | definir `prod` |
| `POSTGRES_DB` | usar nome de banco dedicado |
| `POSTGRES_USER` | usar usuario dedicado |
| `POSTGRES_PASSWORD` | usar segredo forte |
| `APP_JWT_SECRET` | usar chave privada forte (minimo 32 bytes) |
| `APP_PUBLIC_BASE_URL` | usar o dominio publico da API |
| `APP_CORS_ALLOWED_ORIGINS` | restringir aos dominios reais |
| `APP_SEED_ENABLED` | definir `false` |
| `APP_SWAGGER_ENABLED` | definir `false` |
| `APP_ALLOW_PUBLIC_REGISTER` | definir `false` |

## Persistencia de dados

No `docker-compose.yml`, o PostgreSQL usa o volume:

```text
postgres_data
```

Em producao:

- mantenha o volume em disco persistente
- crie rotina de backup do banco
- evite apagar volumes sem necessidade

## Uso de Nginx

O Nginx nao faz parte do repositorio, mas faz sentido em producao para:

- terminacao TLS
- exposicao padronizada em `80` e `443`
- redirecionamento HTTP -> HTTPS
- proxy reverso para a API na porta `8080`
- aplicacao de rate limiting externo

Ao usar Nginx, garanta que:

- o dominio publico usado no proxy seja o mesmo configurado em `APP_PUBLIC_BASE_URL`
- as origens configuradas em `APP_CORS_ALLOWED_ORIGINS` reflitam o dominio real

## Checklist de producao

- usar segredo JWT exclusivo por ambiente
- desativar seed de demonstracao
- desativar Swagger em producao
- desativar cadastro publico de restaurante
- restringir CORS
- configurar HTTPS
- monitorar `GET /actuator/health`
- proteger o acesso ao PostgreSQL
- executar backups regulares
- revisar logs e consumo de disco

## Atualizacao de versao

Fluxo recomendado:

1. atualizar o codigo no servidor
2. rebuildar as imagens
3. subir novamente os containers
4. validar healthcheck e endpoints criticos

Comando:

```bash
docker compose up -d --build
```

## Limites atuais do modelo de deploy

- nao ha pipeline CI/CD versionada no repositorio
- nao ha manifestos Kubernetes
- nao ha migracao versionada de banco
- nao ha configuracao pronta de proxy reverso

Mesmo com essas limitacoes, o repositorio esta pronto para rodar de forma continua em uma VPS com Docker.
