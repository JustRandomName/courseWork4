import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ClientPart {

    private JTextArea area;
    private Socket socket;
    private ArrayList<String> selectFiles;

    private ClientPart() {

        JFrame f = new JFrame("Client");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(300, 200);
        f.setLayout(new BorderLayout());
        f.setVisible(true);

        area = new JTextArea();
        JTextField field = new JTextField(20);
        final JButton selectBut = new JButton("Select");

        final JButton but = new JButton("Send");
        but.setEnabled(false);
        f.add(but, BorderLayout.SOUTH);
        f.add(area);
        f.add(selectBut, BorderLayout.NORTH);

        but.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                sendFiles(selectFiles);
            }
        });

        selectBut.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setMultiSelectionEnabled(true);
                selectFiles = new ArrayList<>();
                area.setText("");
                int returnVal = chooser.showOpenDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    area.append("Выбранны файлы для передачи:\n");
                    File[] file = chooser.getSelectedFiles();
                    for (File d : file) {
                        selectFiles.add(d + "");
                        area.append(d + "\n");
                    }
                    but.setEnabled(true);
                }
            }
        });
    }

    private void sendFiles(ArrayList<String> list) {
        //----------------------------------------------
        int port = 2154;
        String addres = "127.0.0.1";
        InetAddress ipAddress;
        try {
            ipAddress = InetAddress.getByName(addres);
            socket = new Socket(ipAddress, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //----------------------------------------------
        int countFiles = list.size();

        DataOutputStream outD;
        try {
            outD = new DataOutputStream(socket.getOutputStream());

            outD.writeInt(countFiles);//отсылаем количество файлов

            for (String aList : list) {
                File f = new File(aList);

                outD.writeLong(f.length());//отсылаем размер файла
                outD.writeUTF(f.getName());//отсылаем имя файла

                FileInputStream in = new FileInputStream(f);
                byte[] buffer = new byte[64 * 1024];
                int count;

                while ((count = in.read(buffer)) != -1) {
                    outD.write(buffer, 0, count);
                }
                outD.flush();
                in.close();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ClientPart();
    }
}