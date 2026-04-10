import zmq
import msgpack
import time

# ====== CARREGAR DADOS ======
def carregar_usuarios():
    try:
        with open("usuarios.txt", "r") as f:
            return [linha.split(",")[0] for linha in f.readlines()]
    except FileNotFoundError:
        return []

def carregar_canais():
    try:
        with open("canais.txt", "r") as f:
            return [linha.strip() for linha in f.readlines()]
    except FileNotFoundError:
        return []

usuarios = carregar_usuarios()
canais = carregar_canais()

# ====== SALVAR DADOS ======
def salvar_login(usuario):
    with open("usuarios.txt", "a") as f:
        f.write(f"{usuario},{time.time()}\n")

def salvar_canal(canal):
    with open("canais.txt", "a") as f:
        f.write(f"{canal}\n")

# ====== RESPOSTA ======
def criar_resposta(tipo, dados):
    return msgpack.packb({
        "timestamp": time.time(),
        "tipo": tipo,
        "dados": dados
    })

# ====== ZMQ ======
context = zmq.Context()
socket = context.socket(zmq.REP)
socket.connect("tcp://broker:5556")
#nova implementação
# SOCKET PUB (novo)
pub_socket = context.socket(zmq.PUB)
pub_socket.connect("tcp://proxy_pubsub:5557")

print("Servidor rodando...")

#implementação nova
while True:
    mensagem = msgpack.unpackb(socket.recv(), raw=False)
    tipo = mensagem["tipo"]
    dados = mensagem["dados"]

    # LOGIN
    if tipo == "login":
        usuario = dados["usuario"]
        if usuario not in usuarios:
            usuarios.append(usuario)
            salvar_login(usuario)
            resposta = criar_resposta("login", {
                "status": "sucesso",
                "mensagem": "Login realizado"
            })
        else:
            resposta = criar_resposta("login", {
                "status": "erro",
                "mensagem": "Usuário já existe"
            })

    # PUBLICAR MENSAGEM
    elif tipo == "publish":
        canal = dados["canal"]
        mensagem_texto = dados["mensagem"]

        if canal not in canais:
            resposta = criar_resposta("publish", {
                "status": "erro",
                "mensagem": "Canal não existe"
            })
        else:
            payload = {
                "canal": canal,
                "mensagem": mensagem_texto,
                "timestamp_envio": time.time()
            }

            # ENVIA PARA O PROXY
            pub_socket.send_string(f"{canal} {msgpack.packb(payload).hex()}")

            # SALVAR EM DISCO
            with open("mensagens.txt", "a") as f:
                f.write(f"{canal}|{mensagem_texto}|{payload['timestamp_envio']}\n")

            resposta = criar_resposta("publish", {
                "status": "sucesso",
                "mensagem": "Mensagem publicada"
            })

    # CANAL
    elif tipo == "channel":
        canal = dados["nome"]
        if canal not in canais:
            canais.append(canal)
            salvar_canal(canal)
            resposta = criar_resposta("channel", {
                "status": "sucesso",
                "mensagem": "Canal criado"
            })
        else:
            resposta = criar_resposta("channel", {
                "status": "erro",
                "mensagem": "Canal já existe"
            })

    # LISTAR CANAIS
    elif tipo == "listar_canais":
        resposta = criar_resposta("listar_canais", {
            "canais": canais
        })

    # ERRO
    else:
        resposta = criar_resposta("erro", {
            "mensagem": "Comando inválido"
        })

    socket.send(resposta)