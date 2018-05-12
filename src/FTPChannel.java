import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author n.zhuchkevich
 * @version 1.0
 */
public class FTPChannel implements Runnable, DataConnectionListener {
    private SocketChannel channel;
    private Thread thread = null;
    private Scanner reader = null;

    private String username;
    private boolean isAuth = false;
    private boolean isAlive = true;
    @SuppressWarnings("unused")
    private boolean isBinary = true;
    private boolean isUTF8Enable = false;
    private long restart = 0L;
    private DataConnection data = null;
    @SuppressWarnings("unused")
    private InetSocketAddress port = null;

    private File userRoot = null;
    private File userCurrent = null;

    private SimpleDateFormat fmtDate = new SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH);
    private SimpleDateFormat fmtPast = new SimpleDateFormat("MMM dd  yyyy", Locale.ENGLISH);
    private SimpleDateFormat fmtStamp = new SimpleDateFormat("yyyyMMddHHmmss");

    public FTPChannel(SocketChannel channel) throws IOException {
        channel.configureBlocking(true);
        this.channel = channel;
        this.reader = new Scanner(channel, "UTF-8");

        System.out.println("* FTPChannel was created. (" +
                channel.socket().getInetAddress() + ")");

        onConnect();
    }

    /**
     * Start new Channel
     */

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void println(String msg) throws IOException {
        System.out.println("<= " + msg);
        ByteBuffer buf = ByteBuffer.wrap((msg + "\r\n").getBytes("UTF-8"));
        while (buf.hasRemaining()) {
            if (channel == null) {
                break;
            }
            channel.write(buf);
        }
    }

    private void onConnect() throws IOException {
        println("220 Welcome to Rath FTP Daemon.");
    }

    /**
     * get command from user and parse them
     */
    public void run() {
        try {
            while (isAlive) {
                String line = reader.nextLine();
                if (line == null)
                    break;

                System.out.println("=> " + line);

                String cmd = null;
                String param = null;

                int i = line.indexOf(' ');
                if (i != -1) {
                    cmd = line.substring(0, i);
                    param = line.substring(i).trim();
                } else {
                    cmd = line;
                }

                processCommand(cmd, param);
            }
        } catch (NoSuchElementException e) {
            System.out.println("* FTPChannel was closed. (" +
                    channel.socket().getInetAddress() + ")");
        } catch (Exception e) {
            System.err.println("* FTP Channel error occured");
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    /**
     * proxy for command
     */
    private void processCommand(String cmd, String param)
            throws Exception {
        cmd = cmd.toUpperCase();
        if (cmd.equals("USER")) {
            processUser(param);
        } else if (cmd.equals("PASS")) {
            processPassword(param);
        } else if (cmd.equals("AUTH")) {
            processSecurityExtension(param);
        } else if (cmd.equals("SYST")) {
            processSystem();
        } else if (cmd.equals("EPRT") && checkAuth()) {
            processPortExtensionCommand(param);
        } else if (cmd.equals("PORT") && checkAuth()) {
            processPortCommand(param);
        } else if (cmd.equals("PASV") && checkAuth()) {
            processPassive();
        } else if ((cmd.equals("PWD") || cmd.equals("XPWD")) && checkAuth()) {
            processPrintWorkingDirectory();
        } else if (cmd.equals("CWD") && checkAuth()) {
            processChangeWorkingDirectory(param);
        } else if (cmd.equals("TYPE")) {
            processType(param);
        } else if (cmd.equals("OPTS") && checkAuth()) {
            processOptions(param);
        } else if (cmd.equals("LIST") && checkAuth()) {
            processList();
        } else if (cmd.equals("NLST") && checkAuth()) {
            processNameList();
        } else if (cmd.equals("RETR") && checkAuth()) {
            processRetrieve(param);
        } else if (cmd.equals("STOR") && checkAuth()) {
            processStore(param);
        } else if (cmd.equals("SIZE") && checkAuth()) {
            processFileSize(param);
        } else if (cmd.equals("MDTM") && checkAuth()) {
            processFileModifiedTime(param);
        } else if (cmd.equals("REST") && checkAuth()) {
            processFileRestart(param);
        } else if (cmd.equals("NOOP")) {
            println("200 NOOP command successful.");
        } else if ((cmd.equals("MKD") || cmd.equals("XMKD")) && checkAuth()) {
            processDirectoryMake(param);
        } else if ((cmd.equals("RMD") || cmd.equals("XRMD")) && checkAuth()) {
            processDirectoryRemove(cmd, param);
        } else if (cmd.equals("DELE") && checkAuth()) {
            processFileRemove(cmd, param);
        } else if (cmd.equals("CDUP") && checkAuth()) {
            processChangeDirectoryToParent();
        } else if (cmd.equals("QUIT")) {
            processQuit();
        } else {
            println("500 " + cmd + " not understood.");
        }
    }

    private void processSecurityExtension(String param) throws Exception {
        println("502 " + param + " did not implemented.");

    }

    private boolean checkAuth() throws IOException {
        if (!isAuth) {
            println("530 Not logged in.");
            return false;
        }
        return true;
    }

    private void processUser(String username) throws Exception {
        this.username = username;
        println("331 Password required for " + username);
    }

    /**
     * check password and query
     */
    private void processPassword(String password) throws Exception {
        if (username == null) {
            println("503 Bad sequence of commands. Send USER first.");
            return;
        }

        this.isAuth = new Auth().isValidUser(this.username, password);
        if (!isAuth) {
            if (!username.equals("anonymous"))
                Thread.sleep(3000L);
            println("530 Login incorrect.");
        } else {
            userRoot =
                    new File(System.getProperty("ftp.home"), username);
            String privateRoot = System.getProperty("ftp.home." + username);
            if (privateRoot != null) {
                userRoot = new File(privateRoot);
            }

            if (!userRoot.exists()) {
                userRoot.mkdirs();
            }
            userCurrent = userRoot;
            println("230 User " + username + " logged in.");
        }
    }

    private void processSystem() throws Exception {
        println("215 WIN Type: L8");
    }

    private void processOptions(String param) throws Exception {
        String[] params = param.split(" ");
        if (params.length > 1 && params[0].equalsIgnoreCase("UTF8")) {
            String flag = params[1].toUpperCase();
            if (flag.equals("YES") || flag.equals("TRUE") || flag.equals("ON"))
                isUTF8Enable = true;
            else
                isUTF8Enable = false;

            println("200 OPTS UTF8 command successful.");
        } else {
            println("501 Syntax error in parameters or arguments.");
        }
    }

    private void processPortExtensionCommand(String param) throws Exception {
        String[] params = param.split("\\.");
        if (data != null) {
            data.stop();
            data = null;
        }

        this.restart = 0L;
        InetAddress addr = null;
        int port = 0;

        if (params[1].equals("1")) // IPv4
            addr = InetAddress.getAllByName(params[2])[0];
        if (params[1].equals("2")) // IPv6
            addr = InetAddress.getAllByName(params[2])[0];
        port = Integer.parseInt(params[3]);

        InetSocketAddress sock = null;
        try {
            sock = new InetSocketAddress(addr, port);
            println("200 EPRT command successful.");

            this.data = DataConnection.createActive(sock);
            this.data.setFileOffset(restart);
            this.data.addDataConnectionListener(this);
            this.data.start();
        } catch (Exception e) {
            e.printStackTrace();
            println("500 Invalid port format.");
        }

    }

    private void processPortCommand(String param) throws Exception {
        String[] ports = param.split(",");
        if (data != null) {
            data.stop();
            data = null;
        }

        this.restart = 0L;

        InetSocketAddress addr = null;
        try {
            addr = new InetSocketAddress(
                    ports[0] + "." + ports[1] + "." + ports[2] + "." + ports[3],
                    Integer.parseInt(ports[4]) * 256 +
                            Integer.parseInt(ports[5]));

            println("200 PORT command successful.");

            this.data = DataConnection.createActive(addr);
            this.data.setFileOffset(restart);
            this.data.addDataConnectionListener(this);
            this.data.start();
        } catch (Exception e) {
            e.printStackTrace();
            println("500 Invalid port format.");
        }
    }

    private void processPassive() throws Exception {
        if (data != null) {
            data.stop();
            data = null;
        }

        this.restart = 0L;

        data = DataConnection.createPassive();
        data.setFileOffset(restart);
        data.addDataConnectionListener(this);
        data.start();
        println("227 Entering Passive Mode (" + data.getAddressAsString() + ")");
    }

    private void processPrintWorkingDirectory() throws Exception {
        String root = userRoot.getAbsolutePath();
        String curr = userCurrent.getAbsolutePath();

        curr = curr.substring(root.length());
        if (curr.length() == 0) {
            curr = "/";
        }
        curr = curr.replace('\\', '/');

        println("257 \"" + curr + "\" is current directory.");
    }

    private void processChangeWorkingDirectory(String param)
            throws Exception {
        File toChange;
        if (param.length() > 0 && param.charAt(0) == '/') {
            toChange = new File(userRoot, param.substring(1));
        } else {
            toChange = new File(userCurrent, param);
        }

        if (!toChange.exists() || !toChange.isDirectory()) {
            println("550 " + param + ": No such file or directory");
            return;
        }

        String root = userRoot.getAbsolutePath();
        String willChange = toChange.getCanonicalPath();
        if (!willChange.startsWith(root)) {
            println("553 Requested action not taken.");
            return;
        }

        this.userCurrent = new File(willChange);
        println("250 CWD command successful");
    }

    protected void processNameList() throws Exception {
        File[] files = userCurrent.listFiles();
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            sb.append(f.getName()).append("\r\n");
        }
        if (data != null) {
            println("150 Opening ASCII mode data connection for file list");
            data.send(sb.toString(), isUTF8Enable);
        } else {
            println("552 Requested file list action aborted.");
        }

    }

    private void processList() throws Exception {
        File[] files = userCurrent.listFiles();
        StringBuilder sb = new StringBuilder();

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        List<File> list = new ArrayList<File>(files.length);
        list.addAll(Arrays.asList(files));

        list.sort(new Comparator<File>() {
            public int compare(File f0, File f1) {
                return f0.getName().compareTo(f1.getName());
            }
        });

        for (File f : list) {
            if (f.isDirectory()) {
                sb.append("drwxr-xr-x");
            } else if (f.isFile()) {
                sb.append("-rw-r--r--");
            } else
                continue;
            sb.append(' ').append(String.format("%3d", 1))
                    .append(' ').append(String.format("%-8s", this.username))
                    .append(' ').append(String.format("%-8s", this.username)).append(' ');
            long len = f.length();
            if (f.isDirectory())
                len = 4096;
            sb.append(String.format("%8d", len)).append(' ');

            cal.setTimeInMillis(f.lastModified());
            if (cal.get(Calendar.YEAR) == currentYear) {
                sb.append(fmtDate.format(cal.getTime()));
            } else {
                sb.append(fmtPast.format(cal.getTime()));
            }
            sb.append(' ');
            sb.append(f.getName());
            sb.append("\r\n");
        }

        if (data != null) {
            println("150 Opening ASCII mode data connection for file list");
            data.send(sb.toString(), isUTF8Enable);
        } else {
            println("552 Requested file list action aborted.");
        }
    }

    private void processRetrieve(String param) throws Exception {
        File f;
        if (param.charAt(0) == '/')
            f = new File(userRoot, param);
        else
            f = new File(userCurrent, param);

        if (!f.exists()) {
            println("550 " + param + ": No such file or directory");
            if (data != null)
                data.stop();
            return;
        }

        if (data != null) {
            println("150 Opening BINARY mode data connection for " +
                    param + " (" + f.length() + " bytes)");
            data.sendFile(f);
        } else {
            println("552 Requested file action aborted.");
        }
    }

    private void processStore(String param) throws Exception {
        File f = new File(userCurrent, param);

        if (data != null) {
            println("150 Opening BINARY mode data connection for " + param);
            data.storeFile(f);
        } else {
            println("552 Requested file action aborted.");
        }
    }

    private void processFileSize(String param) throws Exception {
        File f;
        if (param.charAt(0) == '/')
            f = new File(userRoot, param);
        else
            f = new File(userCurrent, param);

        if (f.exists()) {
            println("213 " + f.length());
        } else {
            println("550 " + param + ": No such file or directory");
        }
    }

    private void processFileModifiedTime(String param) throws Exception {
        File f = new File(userCurrent, param);
        if (f.exists()) {
            println("213 " + fmtStamp.format(f.lastModified()));
        } else {
            println("550 " + param + ": No such file or directory");
        }
    }

    private void processType(String param) throws Exception {
        param = param.toUpperCase();
        switch (param) {
            case "I":
                isBinary = true;
                break;
            case "A":
                isBinary = false;
                break;
            default:
                println("504 Command not implemented for that parameter.");
                return;
        }

        println("200 Type set to " + param);
    }

    private void processFileRestart(String strOffset) throws Exception {
        long offset = Long.parseLong(strOffset);
        this.restart = offset;
        if (data != null) {
            data.setFileOffset(offset);
        }

        println("350 Restarting at " + offset + ". Send STORE or RETRIEVE to initiate transfer");
    }

    private String getUserPath(File f) throws IOException {
        String root = userRoot.getCanonicalPath();
        String path = f.getCanonicalPath();

        path = path.substring(root.length()).replace('\\', '/');
        if (path.charAt(0) != '/')
            path = '/' + path;
        return path;
    }

    private void processDirectoryMake(String param) throws Exception {
        File f;
        if (param.charAt(0) == '/')
            f = new File(userRoot, param);
        else
            f = new File(userCurrent, param);

        if (f.exists()) {
            println("521 Directory already exists.");
            return;
        }

        if (f.mkdir()) {
            println("257 \"" + getUserPath(f) + "\" - Directory successfully created.");
        } else {
            println("521 Making directory was failed.");
        }
    }

    private void processDirectoryRemove(String cmd, String param)
            throws Exception {
        File f;
        if (param.charAt(0) == '/')
            f = new File(userRoot, param);
        else
            f = new File(userCurrent, param);

        if (!f.exists()) {
            println("521 " + param + ": No such directory.");
            return;
        }

        if (f.isDirectory() && f.delete()) {
            println("250 " + cmd + " command successful.");
        } else {
            println("521 Removing directory was failed.");
        }
    }

    private void processFileRemove(String cmd, String param)
            throws IOException {
        File f;
        if (param.charAt(0) == '/')
            f = new File(userRoot, param);
        else
            f = new File(userCurrent, param);

        if (!f.exists()) {
            println("521 " + param + ": No such directory.");
            return;
        }

        if (f.isFile() && f.delete()) {
            println("250 " + cmd + " command successful.");
        } else {
            println("521 Removing file was failed.");
        }
    }

    private void processChangeDirectoryToParent() throws Exception {
        processChangeWorkingDirectory("..");
    }

    private void processQuit() throws Exception {
        println("221 Goodbye.");
        isAlive = false;
    }

    public void actionNegoatiated(boolean isOk) {
        System.out.println("* Event: action Negotiated: " + isOk);
    }

    public void transferStarted() {
        System.out.println("* Event: transfer Started");
    }

    public void transferCompleted(boolean hasError) {
        System.out.println("* Event: transferCompleted: hasError=" + hasError);
        try {
            if (!hasError)
                println("226 Transfer complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }

        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            channel = null;
        }
    }
}
