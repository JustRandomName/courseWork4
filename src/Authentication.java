import javax.swing.*;
import java.awt.*;

public class Authentication {
    private String login;
    private String password;
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    protected void init(){


        JFrame jFrame = new JFrame("autheuntefication");
        TextField loginLabel = new TextField();
        TextField passLabel = new TextField();
        loginLabel.setSize(10,10);
        passLabel.setSize(10,10);

        jFrame.setSize(300,257);
        jFrame.add(loginLabel);
        jFrame.add(passLabel);
        jFrame.setVisible(true);
    }
}
