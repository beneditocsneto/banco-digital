# Digital Bank API
API REST simplificada para um banco digital, permitindo transferências entre contas e consulta de movimentações.

## Como Rodar o Projeto
### Pré-requisitos
- Java 21+ instalado (`java -version`)
- Maven 3.6+ instalado (`mvn -version`)

### Build
```bash
# Compilar e rodar os testes
./mvnw clean test

# Compilar sem testes
./mvnw clean compile

# Gerar o JAR
./mvnw clean package -DskipTests
```

### Execução
```bash
# Via Maven
./mvnw spring-boot:run

# Via JAR
java -jar target/digital-bank-1.0.0.jar

# Via Docker
docker build -t digital-bank .
docker run -p 8080:8080 digital-bank
```
A aplicação sobe em: **http://localhost:8080**

### Dados Iniciais
Ao subir, 4 contas são criadas automaticamente.

## Documentação (Swagger)
Acesse a UI do Swagger após iniciar a aplicação:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

## Endpoints da API
### Gerenciamento de Contas
#### `POST /api/v1/accounts` - Criar conta
#### `GET /api/v1/accounts` - Listar todas as contas
#### `GET /api/v1/accounts/{accountNumber}` - Buscar conta por número

### Transferências
#### `POST /api/v1/transfers` - Realizar transferência
#### `GET /api/v1/transfers/{externalId}` - Buscar transação pelo externalId
#### `GET /api/v1/transfers/account/{accountNumber}` - Listar transações de uma conta

### Console H2
Acesse o banco em memória em: http://localhost:8080/h2-console

- JDBC URL: `jdbc:h2:mem:digitalbank`
- User: `sa`
- Password: *(vazio)*

## Decisões de Design e Arquitetura
### 1. Notificação de Transferências
Após cada transferência concluída com sucesso, o sistema notifica os observadores registrados através do padrão **Observer**:

- **`TransferEvent`** — record imutável com os dados da transferência (`source`, `target`, `amount`, `currency`)
- **`TransferObserver`** — interface com o método `onTransferCompleted(TransferEvent)`
- **`TransferObservable`** — subject que mantém a lista de observadores e invoca `notify()` ao final de cada transferência

A classe **`NotificacaoServiceImpl`** implementa `TransferObserver` e se registra automaticamente no `TransferObservable` via `@PostConstruct`. O método `onTransferCompleted` é anotado com `@Async`, garantindo que a thread principal não seja bloqueada enquanto a notificação é processada.

Atualmente a notificação é **simulada com logs** (exibindo origem, destino, valor e status), mas o design permanece o mesmo para cenários reais com envio de e-mail, SMS ou notificação push — bastaria substituir a implementação concreta do observer sem alterar o fluxo de transferência.

```
TransferServiceImpl ──notify()──▶ TransferObservable ──▶ TransferObserver (NotificacaoServiceImpl)
                                                         ▶ TransferObserver (futuro: EmailObserver)
                                                         ▶ TransferObserver (futuro: PushObserver)
```

### 2. H2 em Memória
O banco H2 foi escolhido para simplicidade da avaliação. As configurações de batch (`batch_size`, `order_inserts`) otimizam a persistência para o JPA.

**Em produção, migraria para:**
- PostgreSQL ou Oracle (transações fortes e escalabilidade horizontal)
- Flyway ou Liquibase para migrações versionadas

### 3. Estrutura de Pacotes
```
com.bank.digital
├── config          # Configurações (OpenAPI, DataInitializer)
├── controller      # Endpoints REST
├── datasource/
│   ├── entity      # Entidades JPA
│   └── repository  # Repositórios Spring Data
├── dto/
│   ├── request     # Objetos de entrada
│   └── response    # Objetos de saída
├── exception       # Exceções de negócio + handler global
└── service/
    ├── impl        # Implementações concretas
    ├── TransferEvent.java
    ├── TransferObserver.java
    ├── AccountService.java
    └── TransferService.java
```

## Testes
```bash
# Executar todos os testes
./mvnw test

# Executar testes de uma classe específica
./mvnw test -Dtest=TransferServiceTest
```

### Cobertura de Testes

**TransferServiceTest (Service - 15 cenários)**
- Transferência com sucesso (valor normal, mínimo, valor total)
- Conta de origem/destino inexistente
- Saldo insuficiente
- Transação duplicada (idempotência)
- Origem e destino iguais
- Criação de conta (sucesso, saldo zero, duplicada)
- Consultas (transações por conta, por externalId)

**TransferControllerTest (Controller - 5 cenários)**
- Transferência com sucesso (status 201)
- Conta não encontrada (404)
- Saldo insuficiente (400)
- Validação de dados inválidos (400)
- Consultas (GET por externalId, GET por conta)

**AccountControllerTest (Controller - 4 cenários)**
- Criação de conta (201)
- Listagem de contas (200)
- Busca por número (200)
- Validação de dados inválidos (400)
