import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author n.zhuchkevich
 * @version 1.0
 */
class PassiveConnection extends DataConnection {
    private ServerSocketChannel socket;

    PassiveConnection() throws IOException {
        InetAddress local = InetAddress.getLocalHost();

        ServerSocket sock = new ServerSocket();
        int okPort = -1;
        int errorCount = 0;
        while (errorCount < 20) {
            int port = 4096 + (int) (Math.random() * 40000.0D);
            try {
                sock.bind(new InetSocketAddress(local, port));
                okPort = port;
            } catch (IOException e) {
                errorCount++;
                continue;
            }
            break;
        }
        sock.close();

        this.addr = new InetSocketAddress(local, okPort);

        socket = ServerSocketChannel.open();
        socket.configureBlocking(true);
        socket.socket().setSoTimeout(1000 * 10);
        socket.socket().bind(this.addr);
    }

//    protected void doNegotiate() throws IOException {
//        super.channel = SocketChannel.open();
//        super.channel.configureBlocking(true);
//        super.channel.connect(this.addr);
//    }
    protected void doNegotiate() throws IOException {
        super.channel = socket.accept();  //npe
    }

    public void stop() {
        super.stop();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            socket = null;
        }

    }
}
