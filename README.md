# 🎮 ArenaManager: Plataforma de Gestão de Torneios de E-Sports

> **O Desafio:** Plataformas de E-Sports sofrem com picos massivos de tráfego durante a abertura de inscrições para torneios populares. Quedas em integrações bancárias ou lentidão na geração de relatórios não podem paralisar o sistema central.
>
> **A Solução:** O **ArenaManager** é uma arquitetura orientada a microsserviços construída para garantir alta disponibilidade, escalabilidade horizontal sob demanda e isolamento rigoroso de falhas, garantindo que um problema no processamento de pagamentos nunca impeça um jogador de visualizar os torneios ou acessar seu perfil.

---

# 👥 1. Identificação da Equipe (Turma: 26E2_3)

**Responsável pela Organização:** João Victor Martins Pinheiro

| Nome | Papel Inicial | Microsserviço Sob Responsabilidade |
| :--- | :--- | :--- |
| **João Martins** | Responsável pelo API Gateway | `player-service` |
| **Gabriel Maurity** | Responsável pelo Discovery Server | `tournament-service` |
| **Gabriel Consentindo** | Desenvolvedor Backend | `registration-service` |
| **Matheus Horta** | Responsável pelos Bancos de Dados | `payment-service` |
| **Nayanda Robers** | Desenvolvedora Backend | `notification-service` |
| **Rodrigo da Silva** | Responsável pelos Testes e Execução | `analytics-service` |

---

# 🌿 2. Fluxo de Trabalho Git (Git Flow) e Branches

Para manter o repositório organizado, limpo e livre de conflitos destrutivos de código, a equipe adotará um padrão estrito de desenvolvimento. **É terminantemente proibido realizar commits diretos na branch `main`.**

## Padrão de Nomenclatura para Branches

Sempre que um integrante for iniciar o desenvolvimento de sua parte ou corrigir algo, deve criar uma ramificação (*branch*) seguindo a estrutura:

```bash
feat/nome-do-integrante-nome-do-servico
```

ou

```bash
feat/nome-do-integrante-tarefa
```

### Exemplos Práticos

- Trabalho do Gabriel Maurity no Discovery/Tournament:
```bash
feat/gabriel-discovery
```

- Trabalho do Gabriel Consentindo no Registration:
```bash
feat/consentindo-registration
```

- Trabalho da Nayanda no Notification:
```bash
feat/nayanda-notification
```

- Trabalho do Matheus no Payment:
```bash
feat/horta-payment
```

- Trabalho do Rodrigo no Analytics:
```bash
feat/rodrigo-analytics
```

---

## Passo a Passo para Iniciar o seu Trabalho

### 1. Abra o terminal na raiz do projeto

### 2. Certifique-se de estar na branch principal e atualizada

```bash
git checkout main
git pull origin main
```

### 3. Crie e mude para a sua nova branch de desenvolvimento

```bash
git checkout -b feat/seu-nome-seu-servico
```

### 4. Desenvolva as implementações estritamente dentro da pasta do seu microsserviço

### 5. Ao concluir, faça o commit e envie para o repositório remoto

```bash
git add .
git commit -m "feat: implementa funcionalidade X no servico Y"
git push origin feat/seu-nome-seu-servico
```

### 6. Abra um Pull Request (PR) no GitHub

As alterações devem ser revisadas antes de irem para a `main`.

---

# 🏗️ 3. Arquitetura da Solução

O ecossistema base do ArenaManager apoia-se em dois pilares fundamentais do Spring Cloud para suportar a nuvem e a escalabilidade:

## 1. Service Discovery (Eureka Server)

Atua como um catálogo dinâmico. Quando escalamos horizontalmente o `registration-service` para múltiplas instâncias devido a um pico de inscrições, todas se registram automaticamente no Eureka.

Nenhum serviço precisa conhecer o IP fixo ou a porta física do outro em ambiente de produção.

## 2. API Gateway (Spring Cloud Gateway)

É o ponto único de entrada (*Single Point of Entry*) da nossa aplicação.

Ele consulta o Discovery Server em tempo real para rotear as chamadas externas (`/api/*`) para a instância correta e saudável, além de centralizar:

- Segurança
- CORS
- Tratamentos de requisições

---

# 🧩 4. Visão Geral dos Microsserviços

Para garantir que toda a equipe consiga rodar o projeto localmente sem conflitos de portas, adotamos o seguinte padrão:

| Serviço | Porta Padrão | Responsabilidade Principal | Persistência / Banco de Dados | Isolamento Lógico |
| --- | --- | --- | --- | --- |
| `discovery-server` | `8761` | Registro e descoberta dinâmica de serviços | N/A | N/A |
| `api-gateway` | `8080` | Roteamento dinâmico e ponto de entrada único | N/A | N/A |
| `player-service` | `8081` | Gestão de perfis e autenticação de jogadores | PostgreSQL | Schema isolado: `players` |
| `tournament-service` | `8082` | Criação de chaves, regras e cronogramas de jogos | PostgreSQL | Schema isolado: `tournaments` |
| `registration-service` | `8083` | Inscrições de equipes e jogadores em torneios | PostgreSQL | Schema isolado: `registration` |
| `payment-service` | `8084` | Processamento de taxas de inscrição e checkout | PostgreSQL | Schema isolado: `payment` |
| `notification-service` | `8085` | Disparo de alertas, e-mails e webhooks | MongoDB | Instância NoSQL dedicada |
| `analytics-service` | `8086` | Geração de rankings e estatísticas em tempo real | Elasticsearch | Índices isolados otimizados |

---

# 🗄️ 5. Justificativa de Persistência Poliglota

O ArenaManager foge da abordagem monolítica de “um único banco para toda a aplicação”, escolhendo a ferramenta certa para o modelo de dados de cada domínio.

## Elasticsearch (`analytics-service`)

Serviços de estatísticas exigem:

- Agregações complexas
- Ranking dinâmico
- Buscas textuais rápidas
- Alta performance em tempo real

O Elasticsearch armazena documentos indexados de forma otimizada para respostas em milissegundos.

---

## MongoDB (`notification-service`)

Logs de notificações, alertas rápidos e templates de mensagens possuem estruturas altamente dinâmicas.

O MongoDB gerencia documentos JSON de forma nativa e extremamente veloz.

---

## PostgreSQL

Mantido para os serviços transacionais principais:

- `player-service`
- `tournament-service`
- `registration-service`
- `payment-service`

Garantindo:

- Integridade relacional
- Transações ACID
- Schemas isolados

---

# 🛡️ 6. Estratégia de Resiliência (Circuit Breaker)

A comunicação síncrona entre o `registration-service` e o `payment-service` é o ponto mais crítico do sistema.

Se a API de pagamentos externa apresentar lentidão extrema ou cair, as inscrições dos jogadores não podem falhar tragicamente.

Implementamos uma estratégia de **Circuit Breaker com Fallback** através da biblioteca **Resilience4j**.

---

## Regra de Negócio do Fallback

Se a chamada HTTP feita pelo `registration-service` ao `payment-service`:

- Estourar timeout
- Retornar erro 5xx
- Estiver indisponível

O Circuit Breaker abrirá e desviará o fluxo instantaneamente para o método de fallback.

A inscrição será salva localmente com o status:

```json
"AGUARDANDO_PAGAMENTO"
```

---

# 🛠️ Como Simular e Validar a Falha

## 1. Suba todo o ecossistema normalmente

## 2. Derrube o `payment-service`

Pare exclusivamente o container/serviço da porta `8084`.

## 3. Faça uma requisição POST de inscrição

```http
POST http://localhost:8080/api/registrations/
```

## 4. Resultado Esperado

O gateway deve retornar:

- HTTP `201` ou `202`
- Payload contendo:

```json
{
  "status": "AGUARDANDO_PAGAMENTO"
}
```

O console do `registration-service` registrará o acionamento do fallback.

---

# 🚀 7. Instruções de Execução

## Pré-requisitos Obrigatórios

- Java 17
- Apache Maven
- Docker
- Docker Compose

---

# Ordem Correta de Inicialização

## 1. Bancos de Dados e Infraestrutura

Na raiz do projeto:

```bash
docker-compose up -d
```

---

## 2. Discovery Server

Entre na pasta `discovery-server` e execute a aplicação.

Porta:

```txt
8761
```

### Validação

Abra:

```txt
http://localhost:8761
```

---

## 3. API Gateway

Entre na pasta `api-gateway` e execute a aplicação.

Porta:

```txt
8080
```

---

## 4. Microsserviços de Negócio

Inicialize:

- `player-service`
- `tournament-service`
- `registration-service`
- `payment-service`
- `notification-service`
- `analytics-service`

Aguarde entre 30 e 45 segundos e confirme no Eureka que todos estão `UP`.

---

# 🧪 8. Guia de Endpoints e Testes

Todas as requisições externas devem passar pelo:

```txt
http://localhost:8080
```

---

## Criar Novo Jogador (Player Service)

```bash
curl -X POST http://localhost:8080/api/players \
-H "Content-Type: application/json" \
-d '{
  "nome": "Faker",
  "nickname": "Hide on bush",
  "email": "faker@t1.gg"
}'
```

---

## Efetuar Inscrição em Torneio

### Registration Service → Payment Service

```bash
curl -X POST http://localhost:8080/api/registrations \
-H "Content-Type: application/json" \
-d '{
  "playerId": 1,
  "tournamentId": 105,
  "metodoPagamento": "PIX"
}'
```

---

# 📸 9. Evidências Visuais (Placeholders para o TP)

> Atenção equipe: anexem as capturas de tela obrigatórias solicitadas na entrega substituindo as marcações abaixo pelos arquivos reais gerados.

---

## Registro de Serviços no Dashboard do Eureka

*(Inserir imagem aqui)*

---

## Evidência de Funcionamento do Circuit Breaker

### Status `"AGUARDANDO_PAGAMENTO"`

*(Inserir imagem aqui)*

---

## Persistência Poliglota Populada

### Elasticsearch ou MongoDB

*(Inserir imagem aqui)*

---

# ✅ Observações Finais

Se alguém do grupo tiver problemas ao integrar as peças, basta alinhar via Pull Request e revisão de código.
