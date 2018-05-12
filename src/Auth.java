/**
 * @author n.zhuchkevich
 * @version 1.0
 */
public class Auth {
    Auth() {

    }

    /**
     * @param user current user connected (ftp.user."user_name" = "password")
     * @param pass password of current user
     */
    public boolean isValidUser(String user, String pass) {
        return pass.equals(System.getProperty("ftp.user." + user));
    }
}
