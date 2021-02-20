package mediaservice.users;

import com.google.gson.JsonObject;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Log4j2
public class AlexaUserSession extends UserSession {

    public AlexaUserSession(String name, String roomName, String sdpOffer) {
        super(name, roomName);
        this.sdpOffer = sdpOffer;
    }

    public void answerGeneratedForSession(String sdpAnswer, UserRegistry registry, String id) {
        log.info("Returning sdp answer {} to Alexa", sdpAnswer);

        JsonObject startCommunication = new JsonObject();
        startCommunication.addProperty("id", id);
        startCommunication.addProperty("sdpAnswer", sdpAnswer);

        sendMessage(startCommunication, registry);
    }

    public void sendMessage(JsonObject message, UserRegistry registry) {
        WebSocketSession alexaWSSession = registry.getWSSessionByName(this.getName());

        try {
            synchronized (alexaWSSession) {
                alexaWSSession.sendMessage(new TextMessage(message.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
