import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerPart {
    public JTextArea area;

    private ServerPart() {
        JFrame f = new JFrame("Server");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(200, 250);
        f.setLayout(new BorderLayout());

        area = new JTextArea();
        f.add(area);

        f.setAlwaysOnTop(true);
        f.setVisible(true);
        Transfer transfer = new Transfer();
        System.out.println("Start");
        transfer.start();
    }

//    private void connect() {
//        int port = 2154;
//
//        try {
//            ServerSocket ss = new ServerSocket(port);
//            area.append("Wait connect...");
//
//            while (true) {
//                Socket soket = ss.accept();
//
//                InputStream in = soket.getInputStream();
//                DataInputStream din = new DataInputStream(in);
//
//                int filesCount = din.readInt();//получаем количество файлов
//                area.setText("Передается " + filesCount + " файлов\n");
//
//                for (int i = 0; i < filesCount; i++) {
//                    area.append("Прием " + (i + 1) + "вого файла: \n");
//
//                    long fileSize = din.readLong(); // получаем размер файла
//
//                    String fileName = din.readUTF(); //прием имени файла
//                    area.append("Имя файла: " + fileName + "\n");
//                    area.append("Размер файла: " + fileSize + " байт\n");
//
//                    byte[] buffer = new byte[64 * 1024];
//                    FileOutputStream outF = new FileOutputStream(fileName);
//                    int count, total = 0;
//
//                    while ((count = din.read(buffer, 0, Math.min(buffer.length, (int) fileSize - total))) != -1) {
//                        total += count;
//                        outF.write(buffer, 0, count);
//
//                        if (total == fileSize) {
//                            break;
//                        }
//                    }
//                    outF.flush();
//                    outF.close();
//                    area.append("Файл принят\n---------------------------------\n");
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] arg) {
        new ServerPart();
    }
}