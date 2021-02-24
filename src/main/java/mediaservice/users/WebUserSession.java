package mediaservice.users;

import com.google.gson.JsonObject;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class WebUserSession extends UserSession {

    private static final Logger log = LoggerFactory.getLogger(WebUserSession.class);


    private final WebSocketSession session;

    public WebUserSession(WebSocketSession session, String name, String roomName) {
        super(name, roomName);
        this.session = session;

    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getSessionId() {
        return session.getId();
    }

    public void sendMessage(JsonObject message) throws IOException {
        log.debug("Sending message from user '{}': {}", name, message);
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }
}