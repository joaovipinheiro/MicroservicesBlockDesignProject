# 🎮 ArenaManager: Plataforma de Gestão de Torneios de E-Sports

> **O Desafio:** Plataformas de E-Sports sofrem com picos massivos de tráfego durante a abertura de inscrições para torneios populares. Quedas em integrações bancárias ou lentidão na geração de relatórios não podem paralisar o sistema central. 
> 
> **A Solução:** O **ArenaManager** é uma arquitetura orientada a microsserviços construída para garantir alta disponibilidade, escalabilidade horizontal sob demanda e isolamento rigoroso de falhas, garantindo que um problema no processamento de pagamentos nunca impeça um jogador de visualizar os torneios ou acessar seu perfil.

---

## 👥 1. Identificação da Equipe (Turma: 26E2_3)

**Responsável pela Organização:** João Victor Martins Pinheiro

| Nome | Papel Inicial | Microsserviço |
| :--- | :--- | :--- |
| **João Martins** | Responsável pelo API Gateway | `player-service` |
| **Gabriel Maurity** | Responsável pelo Discovery Server | `tournament-service` |
| **Gabriel Consentindo** | Desenvolvedor Backend | `registration-service` |
| **Matheus Horta** | Responsável pelos Bancos de Dados | `payment-service` |
| **Nayanda Robers** | Desenvolvedora Backend | `notification-service` |
| **Rodrigo da Silva** | Responsável pelos Testes e Execução | `analytics-service` |

---

## 🏗️ 2. Arquitetura da Solução



O ecossistema base do ArenaManager apoia-se em dois pilares fundamentais do Spring Cloud para suportar a nuvem e a escalabilidade:

1. **Service Discovery (Eureka Server):** Atua como um catálogo dinâmico. Quando escalamos horizontalmente o `registration-service` para 5 instâncias devido a um pico de inscrições, todas se registram automaticamente no Eureka. Nenhum serviço precisa saber o IP fixo do outro.
2. **API Gateway (Spring Cloud Gateway):** É o ponto único de entrada (Single Point of Entry). Ele consulta o Discovery Server em tempo real para rotear as chamadas externas (`/api/*`) para a instância correta e saudável, além de centralizar segurança, CORS e rate limiting.

---

## 🧩 3. Visão Geral dos Microsserviços

Para garantir que toda a equipe consiga rodar o projeto localmente sem conflitos de portas, adotamos o seguinte padrão:

| Serviço | Porta Padrão | Responsabilidade Principal | Persistência / Banco de Dados | Isolamento Lógico |
| :--- | :---: | :--- | :--- | :--- |
| `discovery-server` | `8761` | Registro e descoberta de serviços. | N/A | N/A |
| `api-gateway` | `8080` | Roteamento dinâmico e ponto de entrada. | N/A | N/A |
| `player-service` | `8081` | Gestão de perfis e autenticação de jogadores. | PostgreSQL | Schema isolado: `players` |
| `tournament-service`| `8082` | Criação de chaves, regras e cronogramas. | PostgreSQL | Schema isolado: `tournaments` |
| `registration-service`| `8083` | Inscrições de equipes/jogadores em torneios. | PostgreSQL | Schema isolado: `registration` |
| `payment-service` | `8084` | Processamento de taxas de inscrição. | PostgreSQL | Schema isolado: `payment` |
| `notification-service`| `8085` | Disparo de alertas, emails e webhooks. | MongoDB | Instância/Database NoSQL dedicado |
| `analytics-service` | `8086` | Geração de rankings e estatísticas em tempo real.| Elasticsearch | Índices isolados |

---

## 🗄️ 4. Justificativa de Persistência Poliglota

O ArenaManager foge da abordagem monolítica de "um banco para tudo", escolhendo a ferramenta certa para o trabalho certo:

* **Elasticsearch (`analytics-service`):** Serviços de estatísticas exigem agregações complexas (ex: win-rate global, ranking de jogadores) e buscas textuais de alta performance em tempo real (fuzzy search). Bancos relacionais sofrem com locks e lentidão em queries de agregação em larga escala. O Elasticsearch indexa documentos de forma otimizada para respostas em milissegundos.
* **MongoDB (`notification-service`):** Logs de notificações, templates de e-mail e históricos de leitura possuem estruturas dinâmicas que mudam constantemente e não necessitam de transações ACID complexas. O MongoDB gerencia documentos JSON com flexibilidade, diferentemente do Redis, que usaremos estritamente como ferramenta transitória (Cache), e não como repositório primário de persistência estruturada.
* **PostgreSQL:** Mantido para os serviços transacionais core (`player`, `tournament`, `registration`, `payment`), garantindo integridade ACID rigorosa, porém particionados via schemas lógicos para evitar acoplamento de dados.

---

## 🛡️ 5. Estratégia de Resiliência (Circuit Breaker)

A comunicação síncrona entre o `registration-service` e o `payment-service` é o ponto crítico do sistema. Se o gateway de pagamento externo cair, as inscrições não podem parar.

Implementamos um **Circuit Breaker com Fallback** (utilizando Resilience4j).

**Regra de Negócio do Fallback:**
Se a chamada ao `payment-service` falhar (Timeout ou Erro 5xx), o Circuit Breaker abre e o fluxo de Fallback é acionado. A inscrição é salva no banco de dados do `registration-service` com o status `"AGUARDANDO_PAGAMENTO"` em vez de falhar a requisição do usuário. Uma rotina assíncrona tentará compensar esse pagamento posteriormente.

### 🛠️ Como Simular a Falha (Para Testes):
1. Suba todo o ecossistema normalmente.
2. Pare a execução exclusivamente do `payment-service` (porta 8084).
3. Faça uma requisição de POST via API Gateway para inscrever um jogador (`POST http://localhost:8080/api/registrations/`).
4. **Resultado Esperado:** O retorno deve ser HTTP 201 (Created) ou HTTP 202 (Accepted), e o JSON de resposta deve conter o campo `status: "AGUARDANDO_PAGAMENTO"`. O console do `registration-service` deve logar a ativação do Fallback.

---

## 🚀 6. Instruções de Execução

### Pré-requisitos
* Java 17 ou superior
* Maven
* Docker e Docker Compose (para subir a infraestrutura de bancos de dados)

### Ordem Correta de Inicialização
Para evitar erros de conexão e falhas de registro, a ordem de *startup* é estrita:

1. **Infraestrutura:** Na raiz do projeto, execute `docker-compose up -d` para subir o Postgres, Mongo e Elasticsearch.
2. **Discovery Server:** Inicie o serviço na porta `8761`.
   * *Validação:* Acesse o dashboard em http://localhost:8761
3. **API Gateway:** Inicie o serviço na porta `8080`.
4. **Microsserviços de Domínio:** Inicie o `player-service`, `tournament-service`, etc. Aguarde cerca de 30 a 60 segundos para que todos apareçam registrados no dashboard do Eureka.

---

## 🧪 7. Guia de Endpoints e Testes

Sempre teste as requisições passando pela porta `8080` (API Gateway) para garantir que o roteamento está funcionando.

**Criar novo Jogador (Player Service)**
```bash
curl -X POST http://localhost:8080/api/players \
-H "Content-Type: application/json" \
-d '{
  "nome": "Faker",
  "nickname": "Hide on bush",
  "email": "faker@t1.gg"
}'