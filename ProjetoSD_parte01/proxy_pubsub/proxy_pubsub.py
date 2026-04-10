import zmq

context = zmq.Context()

# recebe mensagens dos publishers (servidor)
xsub = context.socket(zmq.XSUB)
xsub.bind("tcp://*:5557")

# envia para os subscribers (clientes/bots)
xpub = context.socket(zmq.XPUB)
xpub.bind("tcp://*:5558")

print("Proxy Pub/Sub rodando...")

zmq.proxy(xsub, xpub)