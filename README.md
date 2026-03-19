# Projeto de Sistemas Distribuídos — Parte 1

## 📌 Introdução

Este projeto tem como objetivo implementar um sistema distribuído baseado em troca de mensagens entre clientes e servidores, utilizando um broker intermediário.

Nesta primeira etapa, foram desenvolvidas as seguintes funcionalidades:

- Login de usuários
- Criação de canais
- Listagem de canais

O sistema simula a base de uma aplicação de mensagens distribuída, com comunicação eficiente, persistência de dados e execução em ambiente containerizado.

---

## 🏗️ Arquitetura do Sistema

O sistema é composto por:

- **Broker**: intermedia a comunicação entre clientes e servidores
- **Servidores (2 instâncias)**: processam as requisições
- **Clientes (2 instâncias)**: simulam usuários

### 🔄 Fluxo de comunicação:

Cliente → Broker → Servidor → Broker → Cliente

---

## ⚙️ Tecnologias Utilizadas

### 🔹 Linguagem
- Python 3.11

### 🔹 Comunicação
- ZeroMQ (pyzmq)
  - Padrões utilizados:
    - REQ/REP
    - ROUTER/DEALER

### 🔹 Serialização
- MessagePack (msgpack)

**Justificativa:**  
O MessagePack foi escolhido por ser um formato binário eficiente e compacto, atendendo à exigência do projeto de não utilizar JSON, XML ou texto simples.

### 🔹 Containerização
- Docker
- Docker Compose

---

## 📦 Formato das Mensagens

Todas as mensagens seguem o padrão:

```python
{
  "timestamp": float,
  "tipo": string,
  "dados": dict
}