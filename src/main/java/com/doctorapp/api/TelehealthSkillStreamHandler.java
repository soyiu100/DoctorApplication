package com.doctorapp.api;


import com.doctorapp.data.TelehealthSessionRequest;
import com.doctorapp.room.RoomManager;
import com.doctorapp.room.SessionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
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

        switch (name) {
            case "InitiateSessionWithOffer":
                String sdpOffer = getSdpOffer(payload.path("offer"));
                TelehealthSessionRequest initiateSession = TelehealthSessionRequest
                    .builder()
                    .sessionId("room")
                    .userName("Alexa")
                    .sdpOffer(sdpOffer)
                    .build();
                log.info("Starting initiateSession: " + initiateSession.toString());
                sessionHandler.initiateSessionHandler(initiateSession, roomManager);
                return deferredResponse(skillRequest.path("directive").path("header"));
            case "UpdateSessionWithOffer":
                String updatedSdpOffer = getSdpOffer(payload.path("offer"));
                TelehealthSessionRequest updateSession = TelehealthSessionRequest
                    .builder()
                    .sessionId("room")
                    .userName("Alexa")
                    .sdpOffer(updatedSdpOffer)
                    .build();
                log.info("Starting updateSession: " + updateSession.toString());
                sessionHandler.updateSessionHandler(updateSession, roomManager);
                return deferredResponse(skillRequest.path("directive").path("header"));
            case "SessionDisconnected":
            case "DisconnectSession":
                TelehealthSessionRequest disconnectSession = TelehealthSessionRequest
                    .builder()
                    .sessionId("room")
                    .userName("Alexa")
                    .build();
                sessionHandler.disconnectSessionHandler(disconnectSession, roomManager);
                return deferredResponse(skillRequest.path("directive").path("header"));
            default:
                log.info("Unsupported directive" + name);
                return null;
        }
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
