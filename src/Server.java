import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author n.zhuchkevich
 * @version 1.0
 */
public class Server implements Runnable {
    private ServerSocketChannel socket;

    public Server() {

    }

    public void init() throws IOException {
        this.socket = ServerSocketChannel.open();
        this.socket.configureBlocking(true);
        this.socket.socket().bind(new InetSocketAddress(
                Integer.getInteger("ftp.port", 21))
        );
    }
    /**
     * Init socket and wait connections
     * In case of exception close socket
     * */
    public void run() {
        try {
            while (true) {
                SocketChannel sc = socket.accept();
                new FTPChannel(sc).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            destroy();
        }
    }

    private void destroy() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            this.socket = null;
        }
    }
}
