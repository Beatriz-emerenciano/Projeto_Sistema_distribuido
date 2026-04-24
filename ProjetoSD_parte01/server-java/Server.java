import org.zeromq.ZMQ;
import org.msgpack.core.*;
import org.msgpack.value.*;

public class Server {

    static int clock = 0;
    static int contadorMensagens = 0;
    static String nomeServidor = "servidor-" + System.currentTimeMillis();

    public static void main(String[] args) {

        System.out.println("Servidor Java rodando...");

        ZMQ.Context context = ZMQ.context(1);

        // conexão com broker
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.connect("tcp://broker:5556");

        // conexão com coordenador
        ZMQ.Socket coord = context.socket(ZMQ.REQ);
        coord.connect("tcp://coordenador:5560");

        // 🔥 REGISTRAR SERVIDOR
        registrarServidor(coord);

        while (true) {

            byte[] msg = socket.recv();

            try {
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(msg);
                Value v = unpacker.unpackValue();

                int clockRecebido = 0;

                // pegar clock
                if (v.isMapValue()) {
                    MapValue map = v.asMapValue();

                    for (Value key : map.map().keySet()) {
                        if (key.asStringValue().asString().equals("clock")) {
                            clockRecebido = map.map().get(key).asIntegerValue().asInt();
                        }
                    }
                }

                // atualizar clock lógico
                clock = Math.max(clock, clockRecebido) + 1;

                System.out.println("Clock servidor (recebeu): " + clock);

                // incrementar antes de responder
                clock++;

                // 🔥 CONTADOR DE MENSAGENS
                contadorMensagens++;

                // 🔥 A CADA 10 MENSAGENS → HEARTBEAT
                if (contadorMensagens % 10 == 0) {
                    enviarHeartbeat(coord);
                }

                // resposta
                MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

                packer.packMapHeader(4);

                packer.packString("timestamp");
                packer.packDouble(System.currentTimeMillis() / 1000.0);

                packer.packString("clock");
                packer.packInt(clock);

                packer.packString("tipo");
                packer.packString("resposta");

                packer.packString("dados");
                packer.packString("OK do servidor Java");

                packer.close();

                socket.send(packer.toByteArray());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 🔹 REGISTRO NO COORDENADOR
    public static void registrarServidor(ZMQ.Socket coord) {
        try {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

            packer.packMapHeader(2);

            packer.packString("tipo");
            packer.packString("register");

            packer.packString("dados");
            packer.packMapHeader(1);
            packer.packString("nome");
            packer.packString(nomeServidor);

            packer.close();

            coord.send(packer.toByteArray());

            byte[] reply = coord.recv();

            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(reply);
            Value v = unpacker.unpackValue();

            System.out.println("Registrado no coordenador: " + v);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 HEARTBEAT + RELÓGIO FÍSICO
    public static void enviarHeartbeat(ZMQ.Socket coord) {
        try {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

            packer.packMapHeader(2);

            packer.packString("tipo");
            packer.packString("heartbeat");

            packer.packString("dados");
            packer.packMapHeader(1);
            packer.packString("nome");
            packer.packString(nomeServidor);

            packer.close();

            coord.send(packer.toByteArray());

            byte[] reply = coord.recv();

            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(reply);
            Value v = unpacker.unpackValue();

            // pegar hora correta
            double hora = v.asMapValue()
                .map()
                .get(ValueFactory.newString("dados"))
                .asMapValue()
                .map()
                .get(ValueFactory.newString("hora_correta"))
                .asFloatValue()
                .toDouble();

            System.out.println("Heartbeat OK | Hora sincronizada: " + hora);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}