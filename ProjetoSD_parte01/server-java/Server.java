import org.zeromq.ZMQ;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.Value;

public class Server {
    public static void main(String[] args) {

        System.out.println("Servidor Java rodando...");

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.connect("tcp://broker:5556");

        while (true) {
            byte[] msg = socket.recv();

            try {
                Value v = MessagePack.newDefaultUnpacker(msg).unpackValue();
                System.out.println("Mensagem recebida (MessagePack)");
                System.out.println("Recebido: " + v);
                // RESPOSTA EM MSGPACK (OBRIGATÓRIO)
                MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

                packer.packMapHeader(3);

                packer.packString("timestamp");
                packer.packDouble(System.currentTimeMillis() / 1000.0);

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
}