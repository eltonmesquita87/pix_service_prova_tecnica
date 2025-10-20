# Massas de Dados para Testes de Performance

Este diretório contém as massas de dados utilizadas nos testes de performance do Pix Service.

## Arquivos de Massa

### 1. wallets.csv
**Endpoint**: `POST /wallets`

Contém 50 userIds únicos para criação de carteiras.

**Estrutura**:
```csv
userId
user001
user002
...
user050
```

**Uso no teste**: Cenários de criação de carteiras (Wallet Management, Pix Transfer, Webhook Processing, Withdrawal).

---

### 2. deposits.csv
**Endpoint**: `POST /wallets/{id}/deposit`

Contém 50 valores de depósito variados.

**Estrutura**:
```csv
walletId,amount
1,1000.00
2,1500.50
...
50,2350.00
```

**Faixa de valores**: R$ 500,00 a R$ 3.000,00

**Uso no teste**: Operações de depósito em todos os cenários.

---

### 3. withdrawals.csv
**Endpoint**: `POST /wallets/{id}/withdraw`

Contém 50 valores de saque.

**Estrutura**:
```csv
walletId,amount
1,50.00
2,75.50
...
50,117.00
```

**Faixa de valores**: R$ 25,00 a R$ 150,00

**Uso no teste**: Cenário de Withdrawal Operations.

---

### 4. pix-keys.csv
**Endpoint**: `POST /wallets/{id}/pix-keys`

Contém 50 chaves Pix de diferentes tipos.

**Estrutura**:
```csv
walletId,keyType,keyValue
1,CPF,12345678901
2,EMAIL,pix1@example.com
3,PHONE,11987654321
4,EVP,e2a8b3c4-5d6f-7890-a1b2-c3d4e5f67890
...
```

**Tipos de chave**:
- **CPF**: CPF válido (11 dígitos)
- **EMAIL**: Endereço de e-mail
- **PHONE**: Telefone com DDD (11 dígitos)
- **EVP**: UUID (chave aleatória)

**Distribuição**: 25% de cada tipo de chave

**Uso no teste**: Registro de chaves Pix em todos os cenários.

---

### 5. transfers.csv
**Endpoint**: `POST /pix/transfers`

Contém 50 transferências Pix com dados completos.

**Estrutura**:
```csv
fromWalletId,pixKey,amount,idempotencyKey
1,pix2@example.com,100.00,idempotency-key-001
2,11987654323,150.50,idempotency-key-002
...
```

**Campos**:
- **fromWalletId**: ID da carteira de origem (gerado dinamicamente no teste)
- **pixKey**: Chave Pix do destinatário (CPF, EMAIL, PHONE ou EVP)
- **amount**: Valor da transferência (R$ 75,00 a R$ 300,00)
- **idempotencyKey**: Chave de idempotência única

**Uso no teste**: Cenários Pix Transfer e Webhook Processing.

---

### 6. webhooks.csv
**Endpoint**: `POST /pix/webhook`

Contém 50 eventos de webhook para processamento.

**Estrutura**:
```csv
endToEndId,eventId,eventType,occurredAt
E00000001202510161200001,evt-confirm-001,CONFIRMED,2025-10-16T12:00:00Z
E00000002202510161200002,evt-reject-001,REJECTED,2025-10-16T12:00:01Z
...
```

**Campos**:
- **endToEndId**: ID fim-a-fim da transação (formato E2E do Pix)
- **eventId**: ID único do evento
- **eventType**: Tipo do evento (CONFIRMED ou REJECTED)
- **occurredAt**: Data/hora de ocorrência no formato ISO-8601

**Distribuição**: ~70% CONFIRMED, ~30% REJECTED

**Uso no teste**: Cenário Webhook Processing.

---

## Estratégia de Feeder

Os feeders utilizam a estratégia **circular** (`.circular()`), o que significa que:
- Os dados são lidos de forma sequencial
- Quando chega ao fim do arquivo, volta ao início
- Ideal para testes de longa duração com dados limitados

## Validação dos Dados

Todos os dados foram criados seguindo as especificações da API Swagger:
- Valores mínimos respeitados (amount >= 0.01)
- Formatos de chave Pix válidos
- Chaves de idempotência únicas
- Timestamps em formato ISO-8601

## Como Adicionar Mais Dados

Para aumentar a massa de dados:

1. Mantenha o formato CSV e os headers existentes
2. Adicione novas linhas seguindo o padrão dos dados existentes
3. Para chaves Pix do tipo CPF, use CPFs válidos (com dígitos verificadores corretos)
4. Para EVP, gere UUIDs válidos (formato: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
5. Garanta que chaves de idempotência sejam únicas

## Referência da API

Documentação completa da API disponível em:
```
http://localhost:8080/v2/api-docs
```
