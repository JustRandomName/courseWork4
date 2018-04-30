import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Transfer extends Thread {
    public boolean connect() {
        System.out.println("we Started");
        int port = 2154;
        try {
            ServerSocket ss = new ServerSocket(port);

            while (true) {
                Socket soket = ss.accept();

                InputStream in = soket.getInputStream();
                DataInputStream din = new DataInputStream(in);

                int filesCount = din.readInt();//получаем количество файлов


                for (int i = 0; i < filesCount; i++) {
                    long fileSize = din.readLong(); // получаем размер файла
                    String fileName = din.readUTF(); //прием имени файла

                    byte[] buffer = new byte[64 * 1024];
                    FileOutputStream outF = new FileOutputStream(fileName);
                    int count, total = 0;

                    while ((count = din.read(buffer, 0, Math.min(buffer.length, (int) fileSize - total))) != -1) {
                        total += count;
                        outF.write(buffer, 0, count);
                        Thread.sleep(1000);
                        if (total == fileSize) {
                            System.out.println("File transfer sucseed!!");
                            break;
                        }
                    }
                    outF.flush();
                    outF.close();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    @Override
    public void run(){
        connect();
    }
}
