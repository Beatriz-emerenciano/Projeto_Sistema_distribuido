import org.zeromq.ZMQ;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.Value;
public class Client {
    public static void main(String[] args) {

        System.out.println("Cliente Java iniciado...");

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REQ);
        socket.connect("tcp://broker:5555");

        try {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

            packer.packMapHeader(3);

            packer.packString("timestamp");
            packer.packDouble(System.currentTimeMillis() / 1000.0);
            

            packer.packString("tipo");
            packer.packString("login");

            packer.packString("dados");
            packer.packMapHeader(1);
            packer.packString("usuario");
            packer.packString("Beatriz-Java");

            packer.close();

            socket.send(packer.toByteArray());
             
            byte[] resposta = socket.recv();

// DECODIFICAR A RESPOSTA (IMPORTANTE!)
Value respostaMsg = MessagePack.newDefaultUnpacker(resposta).unpackValue();

System.out.println("Resposta decodificada: " + respostaMsg);
          

        } catch (Exception e) {
            e.printStackTrace();
        }

        socket.close();
        context.term();
    }
}