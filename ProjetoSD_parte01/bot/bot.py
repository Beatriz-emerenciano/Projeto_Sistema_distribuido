import zmq
import msgpack
import time
import random

def criar_mensagem(tipo, dados):
    return msgpack.packb({
        "timestamp": time.time(),
        "tipo": tipo,
        "dados": dados
    })

context = zmq.Context()

# REQ (para servidor)
req = context.socket(zmq.REQ)
req.connect("tcp://broker:5555")

# SUB (para proxy)
sub = context.socket(zmq.SUB)
sub.connect("tcp://proxy_pubsub:5558")

print("Bot iniciado...")

# LOGIN
req.send(criar_mensagem("login", {"usuario": f"bot-{random.randint(1,1000)}"}))
req.recv()

# LISTAR CANAIS
req.send(criar_mensagem("listar_canais", {}))
resposta = msgpack.unpackb(req.recv(), raw=False)
canais = resposta["dados"]["canais"]

# CRIAR CANAIS SE < 5
while len(canais) < 5:
    novo = f"canal-{random.randint(1,100)}"
    req.send(criar_mensagem("channel", {"nome": novo}))
    req.recv()

    req.send(criar_mensagem("listar_canais", {}))
    resposta = msgpack.unpackb(req.recv(), raw=False)
    canais = resposta["dados"]["canais"]

# INSCREVER EM ATÉ 3 CANAIS
inscritos = random.sample(canais, min(3, len(canais)))

for canal in inscritos:
    sub.setsockopt_string(zmq.SUBSCRIBE, canal)

print("Inscrito em:", inscritos)

# LOOP
while True:
    canal = random.choice(canais)

    # envia 10 mensagens
    for _ in range(10):
        texto = f"msg-{random.randint(1,1000)}"

        req.send(criar_mensagem("publish", {
            "canal": canal,
            "mensagem": texto
        }))
        req.recv()

        time.sleep(1)

    # recebe mensagens (não bloqueante)
    try:
     while True:
        msg = sub.recv_string(flags=zmq.NOBLOCK)
        c, payload_hex = msg.split(" ", 1)
        payload = msgpack.unpackb(bytes.fromhex(payload_hex), raw=False)

        print(f"\nCanal: {c}")
        print(f"Mensagem: {payload['mensagem']}")
        print(f"Enviado em: {payload['timestamp_envio']}")
        print(f"Recebido em: {time.time()}")
    except:
      pass
    