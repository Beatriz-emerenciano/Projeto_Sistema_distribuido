
# Projeto de Sistemas Distribuídos
Beatriz Cristina Emerenciano RA: 22.222041-0

Larissa Santos Fiuza RA: 22.123.042-8

## 📌 Introdução

Este projeto foi desenvolvido  com o objetivo de implementar um sistema distribuído capaz de realizar comunicação entre múltiplos clientes e servidores utilizando diferentes linguagens de programação.

A arquitetura do sistema é baseada em um modelo intermediado por um broker, responsável por encaminhar as mensagens entre clientes e servidores. O sistema permite operações como login de usuários, criação de canais e listagem de canais, simulando um ambiente básico de comunicação.

Além disso, o projeto foi desenvolvido considerando requisitos obrigatórios, como o uso de serialização binária e a inclusão de timestamp em todas as mensagens trocadas.

---

## 🏗️ Arquitetura do Sistema

O sistema é composto pelos seguintes componentes:

- **Broker**: responsável por intermediar a comunicação entre clientes e servidores (utilizando ZeroMQ).
- **Cliente 1 (Python)**: envia requisições ao sistema.
- **Servidor 1 (Python)**: processa requisições e gerencia dados.
- **Cliente 2 (Java)**: envia requisições utilizando outra linguagem.
- **Servidor 2 (Java)**: recebe e responde requisições em Java.

A comunicação ocorre da seguinte forma:
Cliente → Broker → Servidor → Broker → Cliente


---

## 💻 Tecnologias Utilizadas

### 🔹 Linguagens de Programação
- **Python**: utilizado para implementação do Cliente 1 e Servidor 1.
- **Java**: utilizado para implementação do Cliente 2 e Servidor 2.

A escolha de múltiplas linguagens foi feita para demonstrar interoperabilidade em sistemas distribuídos.

---

### 🔹 Comunicação

- Utilização da biblioteca **ZeroMQ** para comunicação assíncrona entre os componentes.
- Padrão utilizado:
  - `ROUTER` (lado do broker para clientes)
  - `DEALER` (lado do broker para servidores)
  - `REQ/REP` para clientes e servidores

Essa abordagem permite desacoplamento entre os componentes do sistema.

---

### 🔹 Serialização

Foi utilizada a biblioteca **MessagePack** para serialização das mensagens.
As mensagens seguem uma estrutura padronizada contendo:

- `timestamp`: instante de envio da mensagem
- `tipo`: tipo da operação (ex: login, channel, listar_canais)
- `dados`: conteúdo da mensagem (objeto com informações da operação)

Motivos da escolha:
- Formato binário 
- Alta performance
- Compatibilidade entre múltiplas linguagens (Python e Java)

O uso de MessagePack garante interoperabilidade entre diferentes linguagens **sem** depender de formatos textuais como JSON ou XML.

## 🔗 Interoperabilidade

O sistema permite comunicação entre diferentes combinações de clientes e servidores:

- Python → Python  
- Python → Java  
- Java → Python  
- Java → Java  

Isso é possível graças ao uso de:
- Um protocolo comum (MessagePack)
- Um broker intermediador (ZeroMQ)

---

## ▶️ Como Executar

1. Construir e iniciar os containers:

```bash
Docker compose down
docker compose up --build

