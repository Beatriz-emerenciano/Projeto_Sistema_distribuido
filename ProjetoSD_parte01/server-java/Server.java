import org.zeromq.ZMQ;
import org.msgpack.core.*;
import org.msgpack.value.*;

public class Server {

    // =========================
    // CLOCK LÓGICO
    // =========================
    static int clock = 0;

    // =========================
    // CONTADOR DE MENSAGENS
    // =========================
    static int contadorMensagens = 0;

    // =========================
    // IDENTIFICAÇÃO
    // =========================
    static String nomeServidor =
            "servidor-" + System.currentTimeMillis();

    static int meuRank = 0;

    static String coordenadorAtual = null;

    // =========================
    // HEARTBEAT
    // =========================
    static final int INTERVALO = 2000;

    public static void main(String[] args) {

        System.out.println("Servidor Java rodando...");

        // =========================
        // CONTEXTO ZMQ
        // =========================
        ZMQ.Context context = ZMQ.context(1);

        // =========================
        // SOCKET BROKER
        // =========================
        ZMQ.Socket socket =
                context.socket(ZMQ.REP);

        socket.connect("tcp://broker:5556");

        // =========================
        // SOCKET COORDENADOR
        // =========================
        ZMQ.Socket coord =
                context.socket(ZMQ.REQ);

        coord.connect("tcp://coordenador:5560");

        // =========================
        // REGISTRO
        // =========================
        registrarServidor(coord);

        try {

            Value lista =
                    pedirServidores(coord);

            System.out.println(
                    "Lista recebida: " + lista
            );

            coordenadorAtual =
                    elegerCoordenador(lista);

            System.out.println(
                    "Coordenador atual: "
                            + coordenadorAtual
            );

        } catch (Exception e) {

            e.printStackTrace();
        }

        // =========================
        // THREAD HEARTBEAT
        // =========================
        new Thread(() -> {

            while (true) {

                try {

                    verificarCoordenador(coord);

                    Thread.sleep(INTERVALO);

                } catch (Exception e) {

                    System.out.println(
                            "Erro heartbeat"
                    );
                }
            }

        }).start();

        // =========================
        // LOOP PRINCIPAL
        // =========================
        while (true) {

            byte[] msg = socket.recv();

            try {

                MessageUnpacker unpacker =
                        MessagePack
                                .newDefaultUnpacker(msg);

                Value v =
                        unpacker.unpackValue();

                int clockRecebido = 0;

                // =========================
                // LER CLOCK
                // =========================
                if (v.isMapValue()) {

                    MapValue map =
                            v.asMapValue();

                    for (Value key :
                            map.map().keySet()) {

                        if (key.asStringValue()
                                .asString()
                                .equals("clock")) {

                            clockRecebido =
                                    map.map()
                                            .get(key)
                                            .asIntegerValue()
                                            .asInt();
                        }
                    }
                }

                // =========================
                // CLOCK LÓGICO
                // =========================
                clock =
                        Math.max(
                                clock,
                                clockRecebido
                        ) + 1;

                System.out.println(
                        "Clock servidor: "
                                + clock
                );

                // =========================
                // CONTADOR MENSAGENS
                // =========================
                contadorMensagens++;

                System.out.println(
                        "Mensagens trocadas: "
                                + contadorMensagens
                );

                // =========================
                // BERKELEY A CADA 15 MSG
                // =========================
                if (contadorMensagens >= 10) {

                    sincronizarRelogio(coord);

                    contadorMensagens = 0;
                }

                // =========================
                // RESPOSTA
                // =========================
                MessageBufferPacker packer =
                        MessagePack
                                .newDefaultBufferPacker();

                packer.packMapHeader(4);

                // timestamp
                packer.packString("timestamp");
                packer.packDouble(
                        System.currentTimeMillis()
                                / 1000.0
                );

                // clock
                packer.packString("clock");
                packer.packInt(clock);

                // tipo
                packer.packString("tipo");
                packer.packString("resposta");

                // dados
                packer.packString("dados");
                packer.packString(
                        "OK do servidor Java"
                );

                packer.close();

                socket.send(
                        packer.toByteArray()
                );

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    // =========================
    // REGISTRAR SERVIDOR
    // =========================
    public static void registrarServidor(
            ZMQ.Socket coord
    ) {

        try {

            MessageBufferPacker packer =
                    MessagePack
                            .newDefaultBufferPacker();

            packer.packMapHeader(2);

            packer.packString("tipo");
            packer.packString("register");

            packer.packString("dados");

            packer.packMapHeader(1);

            packer.packString("nome");
            packer.packString(nomeServidor);

            packer.close();

            coord.send(
                    packer.toByteArray()
            );

            byte[] reply = coord.recv();

            MessageUnpacker unpacker =
                    MessagePack
                            .newDefaultUnpacker(reply);

            Value v =
                    unpacker.unpackValue();

            System.out.println(
                    "Registrado: " + v
            );

            MapValue map =
                    v.asMapValue();

            Value dados =
                    map.map().get(
                            ValueFactory
                                    .newString("dados")
                    );

            meuRank =
                    dados.asMapValue()
                            .map()
                            .get(
                                    ValueFactory
                                            .newString("rank")
                            )
                            .asIntegerValue()
                            .asInt();

            System.out.println(
                    "Meu rank: " + meuRank
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================
    // PEDIR SERVIDORES
    // =========================
    public static Value pedirServidores(
            ZMQ.Socket coord
    ) throws Exception {

        MessageBufferPacker packer =
                MessagePack
                        .newDefaultBufferPacker();

        packer.packMapHeader(1);

        packer.packString("tipo");
        packer.packString("get_servers");

        packer.close();

        coord.send(
                packer.toByteArray()
        );

        byte[] reply =
                coord.recv();

        MessageUnpacker unpacker =
                MessagePack
                        .newDefaultUnpacker(reply);

        return unpacker.unpackValue();
    }

    // =========================
    // ELEIÇÃO
    // =========================
    public static String elegerCoordenador(
            Value resposta
    ) {

        int maiorRank = -1;

        String eleito = null;

        MapValue map =
                resposta.asMapValue();

        Value dados =
                map.map().get(
                        ValueFactory
                                .newString("dados")
                );

        if (dados == null
                || !dados.isArrayValue()) {

            return null;
        }

        for (Value item :
                dados.asArrayValue()) {

            MapValue servidor =
                    item.asMapValue();

            String nome =
                    servidor.map()
                            .get(
                                    ValueFactory
                                            .newString("nome")
                            )
                            .asStringValue()
                            .asString();

            int rank =
                    servidor.map()
                            .get(
                                    ValueFactory
                                            .newString("rank")
                            )
                            .asIntegerValue()
                            .asInt();

            if (rank > maiorRank) {

                maiorRank = rank;

                eleito = nome;
            }
        }

        return eleito;
    }

    // =========================
    // HEARTBEAT
    // =========================
    public static void verificarCoordenador(
            ZMQ.Socket coord
    ) {

        try {

            MessageBufferPacker packer =
                    MessagePack
                            .newDefaultBufferPacker();

            packer.packMapHeader(2);

            packer.packString("tipo");
            packer.packString("heartbeat");

            packer.packString("dados");

            packer.packMapHeader(1);

            packer.packString("nome");
            packer.packString(nomeServidor);

            packer.close();

            coord.send(
                    packer.toByteArray()
            );

            coord.recv();

            Value lista =
                    pedirServidores(coord);

            System.out.println(
                    "Lista heartbeat: "
                            + lista
            );

            String novo =
                    elegerCoordenador(lista);

            if (coordenadorAtual == null
                    || !coordenadorAtual.equals(novo)) {

                System.out.println(
                        "Novo coordenador eleito: "
                                + novo
                );

                coordenadorAtual = novo;
            }

            System.out.println(
                    "Coordenador atual: "
                            + coordenadorAtual
            );

        } catch (Exception e) {

            System.out.println(
                    "Falha ao comunicar com coordenador"
            );
        }
    }

    // =========================
    // SINCRONIZAÇÃO BERKELEY
    // =========================
    public static void sincronizarRelogio(
            ZMQ.Socket coord
    ) {

        try {

            MessageBufferPacker packer =
                    MessagePack
                            .newDefaultBufferPacker();

            packer.packMapHeader(1);

            packer.packString("tipo");
            packer.packString("sync_clock");

            packer.close();

            coord.send(
                    packer.toByteArray()
            );

            byte[] reply =
                    coord.recv();

            MessageUnpacker unpacker =
                    MessagePack
                            .newDefaultUnpacker(reply);

            Value resposta =
                    unpacker.unpackValue();

            MapValue map =
                    resposta.asMapValue();

            Value dados =
                    map.map().get(
                            ValueFactory
                                    .newString("dados")
                    );

            int novaHora =
                    dados.asMapValue()
                            .map()
                            .get(
                                    ValueFactory
                                            .newString("hora")
                            )
                            .asIntegerValue()
                            .asInt();

            System.out.println(
                    "Relógio sincronizado com coordenador: "
                            + novaHora
            );

        } catch (Exception e) {

            System.out.println(
                    "Erro sincronização Berkeley"
            );
        }
    }
}