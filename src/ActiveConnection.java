import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author n.zhuchkevich
 * @version 1.0
 */
class ActiveConnection extends DataConnection {
    ActiveConnection() throws IOException {

    }

    protected void doNegotiate() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(true);
        channel.connect(this.addr);
    }
}
