# 🎮 ArenaManager: Plataforma de Gestão de Torneios de E-Sports

![Spring Boot](https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-F2F4F9?style=for-the-badge&logo=spring)
![Kafka](https://img.shields.io/badge/Apache_Kafka-F2F4F9?style=for-the-badge&logo=apache-kafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-F2F4F9?style=for-the-badge&logo=postgresql)
![MongoDB](https://img.shields.io/badge/MongoDB-F2F4F9?style=for-the-badge&logo=mongodb)
![Docker](https://img.shields.io/badge/Docker-F2F4F9?style=for-the-badge&logo=docker)

> **O Desafio:** Plataformas de E-Sports sofrem com picos massivos de tráfego durante a abertura de inscrições para torneios populares. Quedas em integrações bancárias ou lentidão na geração de relatórios não podem paralisar o sistema central.
>
> **A Solução:** O **ArenaManager** é uma arquitetura orientada a microsserviços construída para garantir alta disponibilidade, escalabilidade horizontal sob demanda e isolamento rigoroso de falhas. A plataforma garante que um problema no processamento de pagamentos nunca impeça um jogador de visualizar os torneios ou acessar seu perfil.

---

## 📚 Índice
- [1. Identificação da Equipe](#-1-identificação-da-equipe-turma-26e2_3)
- [2. Estrutura do Projeto](#-2-estrutura-do-projeto)
- [3. Arquitetura da Solução e Tecnologias](#-3-arquitetura-da-solução-e-tecnologias)
- [4. Visão Geral dos Microsserviços](#-4-visão-geral-dos-microsserviços)
- [5. Justificativa de Persistência Poliglota (TP1)](#️-5-justificativa-de-persistência-poliglota-tp1)
- [6. Estratégia de Resiliência: Circuit Breaker (TP1)](#️-6-estratégia-de-resiliência-circuit-breaker-tp1)
- [7. Fluxo de Trabalho Git (Git Flow)](#-7-fluxo-de-trabalho-git-git-flow)
- [8. Instruções de Execução e Variáveis](#-8-instruções-de-execução)
- [9. Guia de Endpoints e Testes](#-9-guia-de-endpoints-e-testes)
- [10. Evidências Visuais e Entregáveis](#-10-evidências-visuais-e-entregáveis)
- [11. Roadmap](#-11-roadmap)

---

## 👥 1. Identificação da Equipe (Turma: 26E2_3)

**Responsável pela Organização:** João Victor Martins Pinheiro

| Nome | Papel Inicial | Microsserviço Sob Responsabilidade |
| :--- | :--- | :--- |
| **João Martins** | Responsável pelo API Gateway e Autenticação | `player-service` / `auth-service` |
| **Gabriel Maurity** | Responsável pelo Discovery Server | `tournament-service` |
| **Gabriel Consentindo** | Desenvolvedor Backend | `registration-service` |
| **Matheus Horta** | Responsável pelos Bancos de Dados | `payment-service` |
| **Nayanda Robers** | Desenvolvedora Backend | `notification-service` |
| **Rodrigo da Silva** | Responsável pelos Testes e Execução | `analytics-service` |

---

## 📁 2. Estrutura do Projeto

```text
ArenaManager/
├── api-gateway/          # Ponto de entrada (Spring Cloud Gateway)
├── discovery-server/     # Service Discovery (Eureka)
├── auth-service/         # JWT e Segurança
├── player-service/       # Gestão de Perfis
├── tournament-service/   # Gestão de Campeonatos
├── registration-service/ # Processo de Inscrições
├── payment-service/      # Pagamentos (Integração Fake)
├── notification-service/ # Disparo de E-mails/Notificações
├── analytics-service/    # Estatísticas (WebFlux + Elasticsearch)
├── docker-compose.yml    # Orquestração de Infraestrutura
└── README.md

```

---

## 🏗️ 3. Arquitetura da Solução e Tecnologias

O ecossistema do ArenaManager apoia-se em padrões modernos de sistemas distribuídos para suportar a nuvem, resiliência e alta escalabilidade:

```text
       Cliente
          │
     API Gateway
          │
 ┌────────┴───────┬────────────────┐
 │                │                │
Auth           Player         Tournament
 │                │                │
Postgres       Postgres         Postgres

       Registration
          │
  [Circuit Breaker]
          │
       Payment
          │
        Kafka
          │
     Notification
 
      Analytics
  (WebFlux + R2DBC)

```

### 🔀 Comunicação entre Serviços

| Origem | Destino | Tipo de Comunicação |
| --- | --- | --- |
| API Gateway | Todos os Serviços | HTTP Síncrono (REST) |
| Registration Service | Payment Service | HTTP Síncrono (OpenFeign) |
| Payment Service | Notification Service | Assíncrono (Apache Kafka) |
| Analytics Service | Outros | HTTP Reativo (WebClient) |

### 3.1. Service Discovery & API Gateway

* **Eureka Server (`discovery-server`):** Atua como um catálogo dinâmico. Quando escalamos um serviço horizontalmente, as instâncias se registram automaticamente. Nenhum serviço precisa conhecer o IP fixo do outro.
* **Spring Cloud Gateway (`api-gateway`):** Ponto único de entrada (*Single Point of Entry*). Roteia as chamadas externas dinamicamente, centralizando CORS e segurança.

### 3.2. Segurança e Autenticação (TP3)

Implementamos um servidor de identidade dedicado (`auth-service`) baseado em **JWT (JSON Web Token)**.

* **Rotas Públicas:** Criação de usuário, login.
* **Rotas Protegidas:** Inscrições, pagamentos e dados de perfil exigem um token Bearer válido no *header* da requisição.
* **Refresh Token:** Fluxo implementado para renovação segura de credenciais expiradas.

**Fluxo de Autenticação:**

```text
Cliente → Login → Auth Service → Gera JWT → Cliente guarda Token
Cliente → Request + Authorization: Bearer → Gateway → Serviços Protegidos

```

### 3.3. Comunicação Assíncrona com Apache Kafka (TP2)

Para reduzir o acoplamento e tolerar falhas, utilizamos mensageria para processos em background.

* **Evento de Domínio:** `PagamentoAprovadoEvent`.
* **Tópico:** `payments.approved`
* **Fluxo:** Quando o `payment-service` (Produtor) processa uma taxa com sucesso, ele publica no tópico. O `notification-service` (Consumidor) escuta este evento e dispara o e-mail.

**Exemplo de Payload no Kafka:**

```json
{
  "paymentId": 15,
  "playerId": 8,
  "tournamentId": 105,
  "status": "APPROVED"
}

```

### 3.4. Observabilidade (TP2)

O comportamento interno do sistema é monitorado de ponta a ponta:

* **Métricas:** Expostas via Spring Boot Actuator/Micrometer (`/actuator/prometheus`, `/actuator/metrics`).
* **Logs e Correlação:** Implementação de logs estruturados utilizando *Correlation IDs*, permitindo rastrear uma requisição.
* *Exemplo:* `INFO [payment-service, traceId=12345] Payment processado com sucesso`



### 3.5. Programação Reativa (TP2)

O sistema incorpora o modelo reativo utilizando **Spring WebFlux** e **Spring Data R2DBC** em rotas de alta concorrência (Analytics), garantindo I/O não bloqueante. O `WebClient` substitui chamadas síncronas pesadas nestes contextos.

**Fluxo Reativo:**

```text
Cliente → Gateway → Analytics (WebFlux) → BD (R2DBC Não-Bloqueante)

```

---

## 🧩 4. Visão Geral dos Microsserviços

Para garantir execução local sem conflitos de portas, adotamos o seguinte padrão:

| Serviço | Porta Padrão | Responsabilidade Principal | Persistência | Isolamento |
| --- | --- | --- | --- | --- |
| `discovery-server` | `8761` | Registro e descoberta dinâmica | N/A | N/A |
| `api-gateway` | `8080` | Roteamento dinâmico e ponto de entrada único | N/A | N/A |
| `auth-service` | `8087` | Emissão de tokens JWT e verificação de identidade | PostgreSQL | Schema: `auth` |
| `player-service` | `8081` | Gestão de perfis e dados de jogadores | PostgreSQL | Schema: `players` |
| `tournament-service` | `8082` | Criação de chaves, regras e cronogramas de jogos | PostgreSQL | Schema: `tournaments` |
| `registration-service` | `8083` | Inscrições de equipes e jogadores em torneios | PostgreSQL | Schema: `registration` |
| `payment-service` | `8084` | Processamento de taxas de inscrição e checkout | PostgreSQL | Schema: `payment` |
| `notification-service` | `8085` | Disparo de e-mails via eventos do Kafka | MongoDB | Instância NoSQL |
| `analytics-service` | `8086` | Geração de rankings e métricas (Reativo) | Elasticsearch | Índices otimizados |

---

## 🗄️ 5. Justificativa de Persistência Poliglota (TP1)

A arquitetura foge da abordagem monolítica, escolhendo a ferramenta certa para cada domínio:

* **Elasticsearch (`analytics-service`):** Exige agregações complexas e ranking dinâmico. O Elasticsearch armazena documentos indexados para respostas textuais e matemáticas em milissegundos.
* **MongoDB (`notification-service`):** Logs de notificações e templates de e-mail possuem estruturas dinâmicas e sem schema rígido. O MongoDB gerencia documentos JSON nativamente e com altíssima velocidade de gravação.
* **PostgreSQL:** Mantido para os serviços transacionais principais (Players, Tournaments, Registrations, Payments, Auth), garantindo integridade relacional, transações ACID e schemas isolados logicamente.

---

## 🛡️ 6. Estratégia de Resiliência: Circuit Breaker (TP1)

A comunicação síncrona via OpenFeign entre o `registration-service` e o `payment-service` é crítica. Se a API de pagamentos apresentar lentidão ou cair, implementamos uma estratégia de **Circuit Breaker com Fallback** (via **Resilience4j**).

**Fluxo de Falha:**

```text
Registration → Tenta chamar Payment → Payment OFF/Timeout → Dispara Fallback local

```

**Regra de Negócio do Fallback:**
A inscrição não será perdida; ela será salva localmente de forma assíncrona com o status `"AGUARDANDO_PAGAMENTO"`. O jogador recebe um HTTP 202 (Accepted) imediatamente.

---

## 🌿 7. Fluxo de Trabalho Git (Git Flow)

**É terminantemente proibido realizar commits diretos na branch `main`.**

Padrão para criação de *branches*: `feat/nome-servico` ou `fix/nome-tarefa`.

1. `git pull origin main`
2. `git checkout -b feat/seu-nome-seu-servico`
3. Commit estruturado: `git commit -m "feat: implementa autenticacao JWT no auth-service"`
4. Abra um **Pull Request (PR)** para revisão obrigatória.

---

## 🚀 8. Instruções de Execução e Variáveis

**Pré-requisitos:** Java 17, Apache Maven, Docker e Docker Compose.

### Variáveis de Ambiente Necessárias (`.env` ou `application.yaml`)

Certifique-se de ter as variáveis configuradas para execução local:

```env
JWT_SECRET=sua_chave_super_secreta_aqui
POSTGRES_USER=postgres
POSTGRES_PASSWORD=admin
SPRING_PROFILES_ACTIVE=dev
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

```

### Ordem Correta de Inicialização

**1. Infraestrutura (Bancos, Kafka, Zookeeper, etc.)**
Na raiz do projeto:

```bash
docker-compose up -d

```

**2. Discovery Server** (Acesse `http://localhost:8761` para validar)
**3. API Gateway** (Porta `8080`)
**4. Microsserviços de Negócio** (`auth`, `player`, `tournament`, `registration`, `payment`, `notification`, `analytics`)
*Aguarde cerca de 45 segundos e confirme no painel do Eureka se todos os serviços constam como `UP`.*

---

## 🧪 9. Guia de Endpoints e Testes

Todas as requisições externas passam pelo API Gateway (`http://localhost:8080`).

### 🔐 9.1. Autenticação (TP3)

**1. Login (Gera o Token JWT):**

```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"email": "faker@t1.gg", "password": "senha_segura"}'

```

*Copie o `accessToken` retornado para usar nas requisições abaixo.*

**2. Refresh Token:**

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
-H "Content-Type: application/json" \
-d '{"refreshToken": "seu_refresh_token_aqui"}'

```

### 👤 9.2. Rota Protegida (Exige JWT)

**Criar/Atualizar Jogador:**

```bash
curl -X POST http://localhost:8080/api/players \
-H "Authorization: Bearer SEU_TOKEN_AQUI" \
-H "Content-Type: application/json" \
-d '{
  "nome": "Faker",
  "nickname": "Hide on bush",
  "email": "faker@t1.gg"
}'

```

### 🏆 9.3. Fluxo de Negócio e Kafka (TP2)

**Efetuar Inscrição (Registration -> Payment -> Kafka -> Notification):**

```bash
curl -X POST http://localhost:8080/api/registrations \
-H "Authorization: Bearer SEU_TOKEN_AQUI" \
-H "Content-Type: application/json" \
-d '{
  "playerId": 1,
  "tournamentId": 105,
  "metodoPagamento": "PIX"
}'

```

*Resultado esperado:* O pagamento é aprovado, um evento é publicado no Kafka, e o console do `notification-service` registra o envio do e-mail.

---

## 📸 10. Evidências Visuais e Entregáveis

> *Atenção equipe: Substituam as marcações abaixo pelos prints reais antes do envio do arquivo `.zip` final.*

### 📍 TP1: Proposta e Arquitetura

* **Serviços Registrados no Eureka:** `[Inserir imagem do Dashboard do Eureka]`
* **Circuit Breaker Atuando (Status `AGUARDANDO_PAGAMENTO`):** `[Inserir imagem da requisição com o Payment derrubado]`
* **Persistência Poliglota Populada:** `[Inserir print do MongoDB Compass ou ElasticHQ]`

### 📍 TP2: Observabilidade, Mensageria e Reatividade

* **Métricas e Logs (Correlation ID visível):** `[Inserir print do console mostrando logs com IDs de rastreamento]`
* **Evento consumido no Kafka:** `[Inserir print do console do notification-service indicando recebimento do PagamentoAprovado]`
* **Endpoint Reativo WebFlux:** `[Inserir print do Postman consumindo um endpoint não-bloqueante]`

### 📍 TP3: Segurança e Autenticação

* **Falha de Acesso (401/403):** `[Inserir print de acesso a rota protegida sem token]`
* **Geração de Token (200 OK):** `[Inserir print do Postman no /auth/login]`
* **Renovação via Refresh Token:** `[Inserir print do Postman no /auth/refresh]`


---

*Projeto acadêmico desenvolvido para a disciplina de Microsserviços e DevOps com Spring Boot e Spring Cloud.*
