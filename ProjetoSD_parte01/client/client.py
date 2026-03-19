import zmq
import msgpack
import time

def criar_mensagem(tipo, dados):
    return msgpack.packb({
        "timestamp": time.time(),
        "tipo": tipo,
        "dados": dados
    })

context = zmq.Context()
socket = context.socket(zmq.REQ)
socket.connect("tcp://broker:5555")

comandos = [
    ("login", {"usuario": "Beatriz"}),
    ("channel", {"nome": "geral"}),
    ("listar_canais", {})
]

for tipo, dados in comandos:
    mensagem = criar_mensagem(tipo, dados)
    socket.send(mensagem)
    resposta = msgpack.unpackb(socket.recv())
    print("Resposta:", resposta)
    time.sleep(0.5)
