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
#foi criaod o canal antes porque Porque o sistema de Pub/Sub exige que o canal exista antes da publicação, garantindo consistência na entrega das mensagens.
comandos = [
    ("login", {"usuario": "Beatriz"}),
    ("channel", {"nome": "geral"}),
    #("listar_canais", {}),
    ("publish", {"canal": "geral", "mensagem": "Teste PubSub"})
]

for tipo, dados in comandos:
    mensagem = criar_mensagem(tipo, dados)
    socket.send(mensagem)

    resposta = msgpack.unpackb(socket.recv(), raw=False)

    #  ATUALIZA O CLOCK CORRETAMENTE
    clock = max(clock, resposta.get("clock", 0)) + 1

    print("Clock atualizado:", clock)
    print("Resposta:", resposta)
    print("-" * 40)

    time.sleep(0.5)