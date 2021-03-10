package com.doctorapp.api;


import com.doctorapp.client.PatientDataClient;
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
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;
import javax.net.ssl.HttpsURLConnection;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
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
                    .sessionId(sessionOptional.get().getRoomId())
                    .userName(sessionOptional.get().getPatientId())
                    .sdpOffer(sdpOffer)
                    .build();
                log.info("Starting initiateSession: " + initiateSession.toString());
                sessionHandler.initiateSessionHandler(initiateSession, roomManager);
            case "UpdateSessionWithOffer":
                String updatedSdpOffer = getSdpOffer(payload.path("offer"));
                TelehealthSessionRequest updateSession = TelehealthSessionRequest
                    .builder()
                    .sessionId(sessionOptional.get().getRoomId())
                    .userName(sessionOptional.get().getPatientId())
                    .sdpOffer(updatedSdpOffer)
                    .build();
                log.info("Starting updateSession: " + updateSession.toString());
                sessionHandler.updateSessionHandler(updateSession, roomManager);
                return deferredResponse(skillRequest.path("directive").path("header"));
            case "SessionDisconnected":
            case "DisconnectSession":
                TelehealthSessionRequest disconnectSession = TelehealthSessionRequest
                    .builder()
                    .sessionId(sessionOptional.get().getRoomId())
                    .userName(sessionOptional.get().getPatientId())
                    .build();
                sessionHandler.disconnectSessionHandler(disconnectSession, roomManager);
                return deferredResponse(skillRequest.path("directive").path("header"));
            default:
                log.info("Unsupported directive" + name);
                return deferredResponse(skillRequest.path("directive").path("header"));
        }
    }


    public String getPatientId(String accessToken) throws IOException {
        String url = "https://telehealth.lucuncai.com/api/patients/accessToken?accessToken=" + accessToken;
        log.info("url is " + url);
        URL u = new URL(url);
        HttpsURLConnection c = (HttpsURLConnection) u.openConnection();
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
        return sb.toString();
//        switch (status) {
//            case 200:
//            case 201:
//                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
//                StringBuilder sb = new StringBuilder();
//                String line;
//                while ((line = br.readLine()) != null) {
//                    sb.append(line).append("\n");
//                }
//                br.close();
//                return sb.toString();
//        }
    }

    private String getSdpOffer(JsonNode offer) {
        String format = offer.path("format").asText();
        if (!"SDP".equals(format)) {
            throw new IllegalArgumentException("Offer must be in SDP format.");
        }
        return offer.path("value").asText();
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
