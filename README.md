# Task Management API

API REST para gerenciamento de tarefas, desenvolvida com **Java 21**, **Spring Boot 3** e **DynamoDB Local**.

---

## Tecnologias

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.2.3 | Framework web |
| AWS SDK DynamoDB | 2.24.12 | Banco de dados NoSQL |
| SpringDoc OpenAPI | 2.3.0 | Documentação Swagger |
| Lombok | latest | Redução de boilerplate |
| JUnit 5 | latest | Testes unitários |
| Mockito | latest | Mocking em testes |
| Docker / Docker Compose | latest | Containerização |

---

## Estrutura do Projeto

```
todo-api/
├── src/
│   ├── main/java/com/desafio/todoapi/
│   │   ├── config/           # Configurações (DynamoDB, OpenAPI)
│   │   ├── controller/       # Camada REST (endpoints HTTP)
│   │   ├── dto/              # Objetos de transferência de dados
│   │   ├── enums/            # Enumerações (Status, Prioridade)
│   │   ├── exception/        # Exceções e handler global
│   │   ├── entity/            # Modelo de dados (entidade DynamoDB)
│   │   ├── repository/       # Camada de acesso a dados
│   │   └── service/          # Regras de negócio
│   └── test/java/com/desafio/todoapi/
│       ├── controller/       # Testes do Controller (MockMvc)
│       └── service/          # Testes do Service e Enums
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Setup e Execução

### Pré-requisitos

- [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/) instalados
- **OU** Java 21 + Maven 3.8+ (para rodar localmente sem Docker)

---

### Opção 1: Docker Compose (Recomendado)

Sobe a API + DynamoDB Local automaticamente:

```bash
# Clonar o repositório
git clone <url-do-repositorio>
cd todo-api

# Subir os serviços
docker-compose up --build

# Para rodar em background
docker-compose up --build -d

# Parar os serviços
docker-compose down
```

A API ficará disponível em: `http://localhost:8080`

---

### Opção 2: Execução Local (sem Docker)

#### 1. Subir o DynamoDB Local

```bash
# Via Docker (apenas o banco)
docker run -p 8000:8000 amazon/dynamodb-local -jar DynamoDBLocal.jar -sharedDb -inMemory
```

#### 2. Rodar a API

```bash
# Compilar e executar
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# OU
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Rodando os Testes

```bash
# Todos os testes
mvn test

# Com relatório de cobertura
mvn test jacoco:report
```

Os testes são **100% unitários** com Mockito — não precisam de DynamoDB rodando.

---

## Documentação da API (Swagger)

Com a aplicação rodando, acesse:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

---

## Endpoints

### Criar Tarefa
```http
POST /tasks
Content-Type: application/json

{
  "title": "Estudar Spring Boot",
  "description": "Revisar conceitos de DynamoDB",
  "priority": "high",
  "due_date": "2026-12-31"
}
```

### Listar Tarefas
```http
GET /tasks
GET /tasks?status=pending
GET /tasks?priority=high
```

### Buscar por ID
```http
GET /tasks/{id}
```

### Atualizar Tarefa
```http
PUT /tasks/{id}
Content-Type: application/json

{
  "title": "Estudar Spring Boot - Atualizado",
  "status": "in_progress"
}
```

### Deletar Tarefa
```http
DELETE /tasks/{id}
```

---

## Modelo de Dados

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Estudar Spring Boot",
  "description": "Revisar conceitos de DynamoDB",
  "status": "pending",
  "priority": "high",
  "due_date": "2026-12-31",
  "created_at": "2026-03-01T10:00:00",
  "updated_at": "2026-03-01T10:00:00"
}
```

---

## Valores Válidos

| Campo | Valores aceitos |
|---|---|
| `status` | `pending`, `in_progress`, `completed`, `cancelled` |
| `priority` | `low`, `medium`, `high` |

---

## Regras de Negócio

- **Título** é obrigatório (mínimo 3, máximo 100 caracteres)
- **Status padrão** ao criar: `pending`
- **Prioridade padrão** ao criar: `medium`
- **Data de vencimento** não pode ser no passado (formato: `yyyy-MM-dd`)
- Tarefas com status **`completed` não podem ser editadas** (somente deletadas)

---

## Respostas de Erro

| HTTP | Situação |
|---|---|
| `400 Bad Request` | Dados inválidos / validação falhou |
| `404 Not Found` | Tarefa não encontrada |
| `422 Unprocessable Entity` | Tarefa completed tentando ser editada |
| `500 Internal Server Error` | Erro inesperado no servidor |

Exemplo de resposta de erro:
```json
{
  "status": 404,
  "message": "Tarefa não encontrada com o ID: abc-123",
  "timestamp": "2026-03-01T10:30:00"
}
```

---

## Guia de Commits (Semantic Commit Messages)

Este projeto segue o padrão de commits semânticos:

| Prefixo | Quando usar |
|---|---|
| `feat:` | Novo recurso para o usuário |
| `fix:` | Correção de bug |
| `docs:` | Alterações na documentação |
| `style:` | Formatação, sem alteração de lógica |
| `refactor:` | Refatoração de código |
| `test:` | Adicionando ou refatorando testes |
| `chore:` | Tarefas de build, configuração etc. |

### Exemplos de commits deste projeto:

```bash
git commit -m "chore: inicializa projeto Spring Boot com dependências do DynamoDB"
git commit -m "feat: cria modelo Task e enums de status e prioridade"
git commit -m "feat: implementa repositório DynamoDB para tarefas"
git commit -m "feat: implementa TaskService com regras de negócio"
git commit -m "feat: implementa TaskController com endpoints CRUD"
git commit -m "feat: adiciona tratamento global de exceções"
git commit -m "feat: configura Swagger/OpenAPI para documentação"
git commit -m "test: adiciona testes unitários do TaskService"
git commit -m "test: adiciona testes unitários do TaskController"
git commit -m "test: adiciona testes unitários dos Enums"
git commit -m "chore: adiciona Dockerfile e docker-compose"
git commit -m "docs: adiciona README com instruções de setup"
```

---

## Testando com curl

```bash
# Criar tarefa
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Minha tarefa","priority":"high","due_date":"2026-12-31"}'

# Listar todas
curl http://localhost:8080/tasks

# Listar por status
curl "http://localhost:8080/tasks?status=pending"

# Buscar por ID
curl http://localhost:8080/tasks/{id}

# Atualizar
curl -X PUT http://localhost:8080/tasks/{id} \
  -H "Content-Type: application/json" \
  -d '{"status":"in_progress"}'

# Deletar
curl -X DELETE http://localhost:8080/tasks/{id}
```
