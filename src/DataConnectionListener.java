/**
 * @author n.zhuchkevich
 * @version 1.0
 */
public interface DataConnectionListener {

    void actionNegoatiated(boolean isOk);

    void transferStarted();

    void transferCompleted(boolean hasError);
}
