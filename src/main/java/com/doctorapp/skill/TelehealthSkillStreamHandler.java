package com.doctorapp.skill;


import com.amazon.ask.model.Response;
import com.doctorapp.api.DisconnectSession;
import com.doctorapp.api.InitiateSession;
import com.doctorapp.data.TelehealthSessionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class TelehealthSkillStreamHandler {

    @PostMapping(value = "/alexa/telehealth/skill/directive",
        consumes = "application/json", produces = "application/json")
    public Response handleRequest(@RequestBody JsonNode skillRequest) {
        log.info("Before, request is " + skillRequest.toString());
        String namespace = skillRequest.get("directive").get("header").get("namespace").asText();
        if (!"Alexa.RTCSessionController".equals(namespace)) {
            throw new IllegalArgumentException("Unsupported namespace: {}" + namespace);
        }
        String name = skillRequest.get("directive").get("header").get("name").asText();
        JsonNode payload = skillRequest.get("directive").get("payload");
        log.info("name is " + name);

        switch (name) {
            case "InitiateSessionWithOffer":
                String sdpOffer = getSdpOffer(payload.get("offer"));
                TelehealthSessionRequest initiateSession = TelehealthSessionRequest
                    .builder()
                    .sessionId("room")
                    .userName("Alexa")
                    .sdpOffer(sdpOffer)
                    .build();
                log.info("initiateSession is " + initiateSession.toString());
                (new InitiateSession()).initiateSessionWithOffer(initiateSession);
                break;
            case "SessionDisconnected":
            case "DisconnectSession":
                TelehealthSessionRequest disconnectSession = TelehealthSessionRequest
                    .builder()
                    .sessionId("room")
                    .userName("Alexa")
                    .build();
                (new DisconnectSession()).disconnectSession(disconnectSession);
                break;
            default:
                log.info("Unsupported directive" + name);
        }
        return null;
    }

    private String getSdpOffer(JsonNode offer) {
        String format = offer.get("format").asText();
        if (!"SDP".equals(format)) {
            throw new IllegalArgumentException("Offer must be in SDP format.");
        }
        return offer.get("value").asText();
    }
}
