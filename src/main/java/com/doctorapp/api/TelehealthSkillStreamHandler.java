package com.doctorapp.api;


import com.doctorapp.client.PatientDataClient;
import com.doctorapp.constant.SessionCallConstants;
import com.doctorapp.data.ScheduledSession;
import com.doctorapp.data.TelehealthSessionRequest;
import com.doctorapp.room.RoomManager;
import com.doctorapp.room.SessionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.net.ssl.HttpsURLConnection;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class TelehealthSkillStreamHandler {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private SessionHandler sessionHandler;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private PatientDataClient patientDataClient;

    @PostMapping(value = "/alexa/telehealth/skill/directive",
        consumes = "application/json", produces = "application/json")
    public ObjectNode handleRequest(@RequestBody JsonNode skillRequest) {
        log.info("Received directive: " + skillRequest.toString());
        String namespace = skillRequest.path("directive").path("header").path("namespace").asText();
        if (!"Alexa.RTCSessionController".equals(namespace)) {
            throw new IllegalArgumentException("Unsupported namespace: {}" + namespace);
        }
        String name = skillRequest.path("directive").path("header").path("name").asText();
        JsonNode payload = skillRequest.path("directive").path("payload");

        String token = skillRequest.path("directive").path("endpoint").path("scope").path("token").asText();
        String patientId = patientDataClient.getPatientIdWithAccessToken(token);
        String userName = patientDataClient.getUserNameWithAccessToken(token);
        log.info("Get patientId: " + patientId);
        Optional<ScheduledSession> sessionOptional = patientDataClient.getCurrentSessionsByPatientId(patientId);

        if (!sessionOptional.isPresent()) {
            return null;
        }
        switch (name) {
            case "InitiateSessionWithOffer":
                String sdpOffer = getSdpOffer(payload.path("offer"));
                TelehealthSessionRequest initiateSession = TelehealthSessionRequest
                    .builder()
                    .roomId(sessionOptional.get().getRoomId())
                    .userName(userName)
                    .sdpOffer(sdpOffer)
                    .build();
                log.info("Starting initiateSession: " + initiateSession.toString());
                String sdpAnswer = sessionHandler.initiateSessionHandler(initiateSession, roomManager);

                callRTCSCAPI(skillRequest, sdpAnswer, userName);

                return deferredResponse(skillRequest.path("directive").path("header"));
            case "UpdateSessionWithOffer":
                String updatedSdpOffer = getSdpOffer(payload.path("offer"));
                TelehealthSessionRequest updateSession = TelehealthSessionRequest
                    .builder()
                    .roomId(sessionOptional.get().getRoomId())
                    .userName(userName)
                    .sdpOffer(updatedSdpOffer)
                    .build();
                log.info("Starting updateSession: " + updateSession.toString());
                String updatedSdpAnswer = sessionHandler.updateSessionHandler(updateSession, roomManager);
                callRTCSCAPI(skillRequest, updatedSdpAnswer, userName);
                return deferredResponse(skillRequest.path("directive").path("header"));
            case "SessionDisconnected":
            case "DisconnectSession":
                TelehealthSessionRequest disconnectSession = TelehealthSessionRequest
                    .builder()
                    .roomId(sessionOptional.get().getRoomId())
                    .userName(userName)
                    .build();
                sessionHandler.disconnectSessionHandler(disconnectSession, roomManager);
                return deferredResponse(skillRequest.path("directive").path("header"));
            default:
                log.info("Unsupported directive" + name);
                return deferredResponse(skillRequest.path("directive").path("header"));
        }
    }

    private String getSdpOffer(JsonNode offer) {
        String format = offer.path("format").asText();
        if (!"SDP".equals(format)) {
            throw new IllegalArgumentException("Offer must be in SDP format.");
        }
        return offer.path("value").asText();
    }

    private void callRTCSCAPI(JsonNode skillRequest, String sdpAnswer, String userName) {

        ObjectNode header = (ObjectNode) skillRequest.path("directive").path("header");
        header.put("name", "AnswerGeneratedForSession");

        ObjectNode answerValue = mapper.createObjectNode();
        answerValue.put("format", "SDP");
        answerValue.put("value", sdpAnswer);

        ObjectNode payloadValue = mapper.createObjectNode();
        payloadValue.put("sessionId", skillRequest.path("directive").path("payload").path("sessionId").asText());
        payloadValue.put("sessionDomain", skillRequest.path("directive").path("payload").path("sessionDomain").asText());
        payloadValue.putPOJO("answer", answerValue);

        ObjectNode eventValue = mapper.createObjectNode();
        eventValue.putPOJO("header", header);
        eventValue.putPOJO("payload", payloadValue);

        ObjectNode rtcscRequest = mapper.createObjectNode();
        rtcscRequest.putPOJO("event", eventValue);
        log.info("rtcscRequest is " + rtcscRequest.toString());

        String lwaToken = getLWAToken(userName);
        log.info("Got LWA Token: " + lwaToken);

        try {
            URL rtcscUrl = new URL("https://api.amazonalexa.com/v1/rtcsessioncontroller/events");
            HttpsURLConnection c = (HttpsURLConnection) rtcscUrl.openConnection();
            c.setDoOutput(true);
            log.info("Making HTTP call to URL: " + rtcscUrl);
            c.setRequestMethod("POST");
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Authorization", "Bearer " + lwaToken);
            c.connect();
            OutputStream os = c.getOutputStream();
            os.write(rtcscRequest.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            int status = c.getResponseCode();
            log.info("RTCSC response status: " + status );
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            log.info("Got response from APIGW: " + sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLWAToken(String userName) {
        try {
            URL accessTokenUrl = new URL(SessionCallConstants.TOKEN_URL);
            JSONObject accessTokenResult = getHttpPost(accessTokenUrl);
            String accessToken = accessTokenResult.get("access_token").toString();

            URL lwaUrl = new URL(String.format(SessionCallConstants.LWA_TOKEN_URL, userName, accessToken));
            JSONObject lwaResult = getHttpPost(lwaUrl);
            log.info("Get LWA result: " + lwaResult.toString());
            return lwaResult.get("access_token").toString();
        } catch (IOException e) {
            log.info(e);
        }
        return null;
    }

    private JSONObject getHttpPost(URL u) throws IOException {
        HttpsURLConnection c = (HttpsURLConnection) u.openConnection();
        log.info("Making HTTP call to URL: " + u);
        c.setRequestMethod("POST");
        c.connect();
        int status = c.getResponseCode();

        log.info("status is " + status);
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();

        log.info("Got response: " + sb.toString());
        return new JSONObject(sb.toString());
    }

    private ObjectNode deferredResponse(JsonNode headerJson) {
        // Construct header
        ObjectNode header = (ObjectNode) headerJson;
        header.put("namespace", "Alexa");
        header.put("name", "DeferredResponse");
        // Construct payload
        ObjectNode payloadValue = mapper.createObjectNode();
        payloadValue.put("estimatedDeferralInSeconds", 7);
        // Construct event
        ObjectNode eventValue = mapper.createObjectNode();
        eventValue.putPOJO("header", header);
        eventValue.putPOJO("payload", payloadValue);
        // Construct deferredResponse
        ObjectNode deferredResponse = mapper.createObjectNode();
        deferredResponse.putPOJO("event", eventValue);
        log.info("Return DeferredResponse " + deferredResponse.toString());
        return deferredResponse;
    }
}
