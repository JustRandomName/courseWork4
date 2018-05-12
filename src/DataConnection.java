import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author n.zhuchkevich
 * @version 1.0
 */
public abstract class DataConnection implements Runnable {
    protected SocketChannel channel;
    protected InetSocketAddress addr;
    private Thread thread = null;

    private boolean isNego = false;
    private List<DataConnectionListener> listeners =
            Collections.synchronizedList(new ArrayList<DataConnectionListener>());
    private Object lock = new Object();
    private boolean notified = false;
    private ByteBuffer toWrite = null;
    private File fileSend = null;
    private File fileReceive = null;
    private long offset = 0L;

    public static DataConnection createPassive() throws IOException {
        return new PassiveConnection();
    }

    public static DataConnection createActive(InetSocketAddress dest)
            throws IOException {
        ActiveConnection ac = new ActiveConnection();
        ac.addr = dest;
        return ac;
    }

    protected DataConnection() {

    }

    public void setFileOffset(long offset) {
        this.offset = offset;
    }

    public long getFileOffset() {
        return this.offset;
    }

    public InetSocketAddress getAddress() {
        return this.addr;
    }


    public String getAddressAsString() {
        int port = addr.getPort();
        String[] ips = addr.getAddress().getHostAddress().split("\\.");
        return ips[0] + "," + ips[1] + "," + ips[2] + "," + ips[3] +
                "," + (port / 256) + "," + (port % 256);
    }

    public void addDataConnectionListener(DataConnectionListener l) {
        if (!listeners.contains(l))
            listeners.add(l);
    }

    public void removeDataConnectionListener(DataConnectionListener l) {
        listeners.remove(l);
    }

    protected abstract void doNegotiate() throws IOException;

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
            channel = null;
        }

        if (thread != null) {
            thread.interrupt();
            thread = null;
        }

    }

    public void run() {
        FileChannel file = null;
        try {
            isNego = false;
            doNegotiate();
            isNego = true;
            for (DataConnectionListener l : listeners)
                l.actionNegoatiated(true);

            synchronized (lock) {
                if (!notified) {
//					System.out.println( "DEBUG: lock.wait()" );
                    lock.wait(1000 * 8);
                }
            }

            for (DataConnectionListener l : listeners)
                l.transferStarted();

            if (toWrite != null) {
                while (toWrite.hasRemaining())
                    channel.write(toWrite);
            }

            if (fileSend != null) {
                ByteBuffer buf = ByteBuffer.allocateDirect(16384);
                file = new FileInputStream(fileSend).getChannel();
                file.position(offset);
                while (true) {
                    buf.clear();
                    int readlen = file.read(buf);
                    if (readlen < 1)
                        break;
                    buf.flip();
                    while (buf.hasRemaining())
                        channel.write(buf);
                }
            }

            if (fileReceive != null) {
                ByteBuffer buf = ByteBuffer.allocateDirect(16384);
                file = new FileOutputStream(fileReceive).getChannel();
                while (true) {
                    buf.clear();
                    int readlen = channel.read(buf);
                    if (readlen < 1)
                        break;
                    buf.flip();
                    while (buf.hasRemaining())
                        file.write(buf);
                }
            }

            for (DataConnectionListener l : listeners)
                l.transferCompleted(false);
        } catch (InterruptedException e) {
            if (!isNego) {
                for (DataConnectionListener l : listeners)
                    l.actionNegoatiated(false);
            } else {
                for (DataConnectionListener l : listeners)
                    l.transferCompleted(true);
            }
        } catch (Exception e) {
            e.printStackTrace();

            if (!isNego) {
                for (DataConnectionListener l : listeners)
                    l.actionNegoatiated(false);
            } else {
                for (DataConnectionListener l : listeners)
                    l.transferCompleted(true);
            }
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                file = null;
            }
            stop();
        }
    }


    public boolean isNegotiated() {
        return this.isNego;
    }

    public void send(String msg, boolean isUTF8) throws IOException {
        this.toWrite = ByteBuffer.wrap(msg.getBytes(
                isUTF8 ? "UTF-8" : System.getProperty("client.file.encoding")));
        synchronized (lock) {
            lock.notify();
        }
        this.notified = true;
// System.out.println( "DEBUG: lock.notify()" );
    }

    public void sendFile(File f) {
        this.toWrite = null;
        this.fileSend = f;
        synchronized (lock) {
            lock.notify();
        }
        this.notified = true;
//		System.out.println( "DEBUG: lock.notify()" );
    }

    public void storeFile(File f) {
        this.toWrite = null;
        this.fileReceive = f;
        synchronized (lock) {
            lock.notify();
        }
        this.notified = true;
//		System.out.println( "DEBUG: lock.notify()" );
    }
}
