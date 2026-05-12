import zmq
import msgpack
import time
import threading

context = zmq.Context()

# =========================
# SOCKET CLIENTES (BROKER)
# =========================
socket = context.socket(zmq.REP)
socket.connect("tcp://broker:5556")

# =========================
# SOCKET COORDENADOR
# =========================
coord = context.socket(zmq.REQ)
coord.connect("tcp://coordenador:5560")

# =========================
# PUB / SUB (REPLICAÇÃO + ELEIÇÃO)
# =========================
pub = context.socket(zmq.PUB)
pub.connect("tcp://proxy_pubsub:5557")

sub = context.socket(zmq.SUB)
sub.connect("tcp://proxy_pubsub:5558")

# canais
sub.setsockopt_string(zmq.SUBSCRIBE, "servers")
sub.setsockopt_string(zmq.SUBSCRIBE, "replica")

# =========================
# IDENTIFICAÇÃO
# =========================
nome_servidor = f"servidor-py-{time.time()}"

meu_rank = 0
coordenador_atual = None

# =========================
# CLOCK / ESTADO
# =========================
clock = 0
contador_mensagens = 0
INTERVALO = 2

# =========================
# HISTÓRICO (PARTE 5)
# =========================
historico = []

# =========================
# REGISTRO
# =========================
def registrar():

    global meu_rank

    msg = {
        "tipo": "register",
        "dados": {"nome": nome_servidor}
    }

    coord.send(msgpack.packb(msg))
    resposta = msgpack.unpackb(coord.recv(), raw=False)

    meu_rank = resposta["dados"]["rank"]
    print("Registrado com rank:", meu_rank)


# =========================
# LISTA SERVIDORES
# =========================
def pedir_servidores():

    msg = {"tipo": "get_servers"}

    coord.send(msgpack.packb(msg))
    return msgpack.unpackb(coord.recv(), raw=False)


# =========================
# ELEIÇÃO
# =========================
def eleger(lista):

    maior = -1
    eleito = None

    for srv in lista["dados"]:
        if srv["rank"] > maior:
            maior = srv["rank"]
            eleito = srv["nome"]

    return eleito


# =========================
# PUBLICAR COORDENADOR
# =========================
def publicar_coordenador(nome):

    payload = {"coordenador": nome}
    mensagem = "servers " + msgpack.packb(payload).hex()

    pub.send_string(mensagem)
    print("Coordenador publicado:", nome)


# =========================
# PUB/SUB LISTENER
# =========================
def ouvir_pubsub():

    global coordenador_atual

    while True:
        try:
            msg = sub.recv_string()
            canal, payload_hex = msg.split(" ", 1)

            payload = msgpack.unpackb(
                bytes.fromhex(payload_hex),
                raw=False
            )

            # =========================
            # ELEIÇÃO
            # =========================
            if canal == "servers":
                coordenador_atual = payload["coordenador"]
                print("Novo coordenador:", coordenador_atual)

            # =========================
            # REPLICAÇÃO (PARTE 5)
            # =========================
            elif canal == "replica":

                dado = payload["dados"]

                historico.append(dado)

                print("📦 Replica recebida:", dado)

        except Exception as e:
            print("Erro SUB:", e)


# =========================
# HEARTBEAT / ELEIÇÃO
# =========================
def heartbeat():

    global coordenador_atual

    while True:
        try:

            msg = {
                "tipo": "heartbeat",
                "dados": {"nome": nome_servidor}
            }

            coord.send(msgpack.packb(msg))
            coord.recv()

            lista = pedir_servidores()
            novo = eleger(lista)

            if coordenador_atual != novo:

                coordenador_atual = novo
                publicar_coordenador(novo)

                print("REQ eleição")
                print("REP OK")

            print("Coordenador atual:", coordenador_atual)

        except Exception as e:
            print("Erro heartbeat:", e)

        time.sleep(INTERVALO)


# =========================
# SINCRONIZAÇÃO CLOCK
# =========================
def sincronizar_relogio():

    global clock

    try:

        msg = {"tipo": "sync_clock"}

        coord.send(msgpack.packb(msg))
        resposta = msgpack.unpackb(coord.recv(), raw=False)

        clock = max(clock, int(resposta["dados"]["hora"]))

        print("Relógio sincronizado:", clock)

    except Exception as e:
        print("Erro sync:", e)


# =========================
# INICIALIZAÇÃO
# =========================
registrar()

lista = pedir_servidores()
coordenador_atual = eleger(lista)

print("Coordenador inicial:", coordenador_atual)

threading.Thread(target=heartbeat, daemon=True).start()
threading.Thread(target=ouvir_pubsub, daemon=True).start()


# =========================
# LOOP PRINCIPAL (CLIENTES)
# =========================
while True:

    try:

        msg = msgpack.unpackb(socket.recv(), raw=False)

        clock_recebido = msg.get("clock", 0)
        clock = max(clock, clock_recebido) + 1

        contador_mensagens += 1

        # =========================
        # SALVA HISTÓRICO LOCAL
        # =========================
        historico.append({
            "clock": clock,
            "msg": msg
        })

        # =========================
        # REPLICA PARA OUTROS SERVIDORES
        # =========================
        replica = {
            "tipo": "replica",
            "dados": {
                "clock": clock,
                "msg": msg
            }
        }

        pub.send_string("replica " + msgpack.packb(replica).hex())

        print("Clock:", clock)
        print("Mensagens:", contador_mensagens)

        # sincroniza
        if contador_mensagens >= 15:
            sincronizar_relogio()
            contador_mensagens = 0

        resposta = {
            "clock": clock,
            "tipo": "resposta",
            "dados": "OK servidor Python"
        }

        socket.send(msgpack.packb(resposta))

    except Exception as e:
        print("Erro servidor:", e)