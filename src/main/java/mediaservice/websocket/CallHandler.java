package mediaservice.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import mediaservice.room.Room;
import mediaservice.room.RoomManager;
import mediaservice.users.UserRegistry;
import mediaservice.users.UserSession;
import mediaservice.users.WebUserSession;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class CallHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CallHandler.class);
    private static final Gson gson = new GsonBuilder().create();
    private static final String tokenEndpoint = "https:///api/partner/token?user_id=user&partner_id=qiang_telehealth_skill_5";
    private String appToken = "Bearer b326f600-938d-4cc9-a80c-c51ea965f9c7";

    @Autowired
    private UserRegistry registry;

    @Autowired
    private RoomManager roomManager;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
        UserSession user = registry.getBySession(session);
        log.info("Get websocket message from session: {}", session.getId());

        if (user != null) {
            log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
        } else {
            log.debug("Incoming message from new user: {}", jsonMessage);
        }

        switch (jsonMessage.get("id").getAsString()) {
            case "register":
                try {
                    registerWebUser(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "registerResponse");
                }
                break;
            case "providerJoinSession":
                joinRoom(session, jsonMessage);
                providerJoinRoom(jsonMessage.getAsJsonPrimitive("room").getAsString());
                break;
            case "onIceCandidate":
                // Received local candidate
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                if (user != null) {
                    IceCandidate iceCandidate = new IceCandidate(
                        candidate.get("candidate").getAsString(),
                        candidate.get("sdpMid").getAsString(),
                        candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(iceCandidate);
                }
                break;
            case "updatedSdpAnswer":
                String updatedSdpAnswer = jsonMessage.getAsJsonPrimitive("sdpAnswer").getAsString();
                String roomName = jsonMessage.getAsJsonPrimitive("room").getAsString();
                reNegotiateWithSdpAnswer(roomName, updatedSdpAnswer);
                break;
            case "leave":
                leaveRoom(session, jsonMessage.getAsJsonPrimitive("room").getAsString());
                break;
            case "terminate":
                terminate(jsonMessage.getAsJsonPrimitive("room").getAsString());
                break;
            default:
                break;
        }
    }

    // If doctor join later
    private void providerJoinRoom(String roomName) {
        Room room = roomManager.getRoomOrCreate(roomName);

        WebUserSession provider = room.getProvider();

        // Start the session
        WebRtcEndpoint providerWebRtcEp = room.createProviderWebRtcEp(provider);

        if (room.getAlexaWebRtcEp() != null) {
            room.setUpPipeline();
        }

        // Generate SDP answer for caller(Provider)
        log.info("Provider SDP: {} ", provider.getSdpOffer());
        String callerSdpAnswer = providerWebRtcEp.processOffer(provider.getSdpOffer());
        JsonObject startCommunication = new JsonObject();
        startCommunication.addProperty("id", "startCommunication");
        startCommunication.addProperty("sdpAnswer", callerSdpAnswer);

        try {
            provider.sendMessage(startCommunication);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("SDP answer is generated for provider");
        providerWebRtcEp.gatherCandidates();
    }

    private void reNegotiateWithSdpAnswer(String roomName, String updatedSdpAnswer) {
        Room room = roomManager.getRoomOrThrow(roomName);

        room.getProviderWebRtcEp().processAnswer(updatedSdpAnswer);
        log.info("Updated SDP answer has been processed by provider");
    }

    private void handleErrorResponse(Throwable throwable, WebSocketSession session,
        String responseId)
        throws IOException {
        log.error(throwable.getMessage(), throwable);
        JsonObject response = new JsonObject();
        response.addProperty("id", responseId);
        response.addProperty("response", "rejected");
        response.addProperty("message", throwable.getMessage());
        session.sendMessage(new TextMessage(response.toString()));
    }

    private void registerWebUser(WebSocketSession session, JsonObject jsonMessage)
        throws IOException {
        String name = jsonMessage.getAsJsonPrimitive("provider").getAsString();

        WebUserSession caller = new WebUserSession(session, name, null);
        String responseMsg = "accepted";
        registry.registerWebUser(caller);

        log.info("Web user {} has been registered successfully, sessionId is {}", name,
            session.getId());
        JsonObject response = new JsonObject();
        response.addProperty("id", "registerResponse");
        response.addProperty("response", responseMsg);
        caller.sendMessage(response);
    }

    private WebUserSession joinRoom(WebSocketSession session, JsonObject jsonMessage) {

        String providerName = jsonMessage.getAsJsonPrimitive("provider").getAsString();
        String roomName = jsonMessage.getAsJsonPrimitive("room").getAsString();
        String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();

        WebUserSession caller = new WebUserSession(session, providerName, roomName);
        registry.registerWebUser(caller);
        log.info("Web user {} has been registered successfully, sessionId is {}", providerName,
            caller.getSessionId());
        Room room = roomManager.getRoomOrCreate(roomName);
        caller.setSdpOffer(sdpOffer);
        room.joinAsProvider(providerName, caller);
        caller.setRoomName(roomName);
        return caller;
    }

    public void leaveRoom(WebSocketSession session, String roomName) {
        Room room = roomManager.getRoomOrThrow(roomName);

//        try {
//            String lwaToken = getLWAToken();
//            log.info("Get Application token: {}", lwaToken);
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }

        // Show waiting room if patient not null
        if (room.getAlexaWebRtcEp() != null) {
            room.buildWelcomeConnection(room.getAlexaWebRtcEp());
        }
        room.disconnectProvider();
        registry.removeBySession(session);
        if (room.getProviderWebRtcEp() == null && room.getAlexaWebRtcEp() == null) {
            roomManager.removeRoom(room);
        }
    }

    private void terminate(String roomName) {
        Room room = roomManager.getRoomOrThrow(roomName);
        roomManager.removeRoom(room);
    }

    public String getLWAToken() throws IOException {
        URL u = new URL(tokenEndpoint);
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + appToken);
        c.setUseCaches(false);
        c.setConnectTimeout(1000);
        c.setReadTimeout(1000);
        c.connect();
        int status = c.getResponseCode();

        switch (status) {
            case 200:
            case 201:
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                return sb.toString();
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Connection is closed###################### for session " + session.getId());
//		UserSession stopperUser = registry.getBySession(session);
//		Room room = roomManager.getRoomOrCreate(stopperUser.getRoomName());
//		roomManager.removeRoom(room);
    }
}