import zmq
import msgpack
import time

clock = 0

def criar_mensagem(tipo, dados):
    global clock
    clock += 1

    return msgpack.packb({
        "timestamp": time.time(),
        "clock": clock,
        "tipo": tipo,
        "dados": dados
    })

context = zmq.Context()
socket = context.socket(zmq.REQ)
socket.connect("tcp://broker:5555")

# LISTA GRANDE DE COMANDOS
comandos = []

# login inicial
comandos.append(("login", {"usuario": "Beatriz"}))
comandos.append(("channel", {"nome": "geral"}))

# envia MUITAS mensagens
for i in range(20):
    comandos.append((
        "publish",
        {
            "canal": "geral",
            "mensagem": f"Mensagem {i}"
        }
    ))

# EXECUTA
for tipo, dados in comandos:

    mensagem = criar_mensagem(tipo, dados)

    socket.send(mensagem)

    resposta = msgpack.unpackb(
        socket.recv(),
        raw=False
    )

    # CLOCK LAMPORT
    clock = max(
        clock,
        resposta.get("clock", 0)
    ) + 1

    print("Clock atualizado:", clock)
    print("Resposta:", resposta)
    print("-" * 40)

    time.sleep(0.3)