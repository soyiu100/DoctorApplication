package com.doctorapp.api;

import com.google.gson.JsonObject;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import com.doctorapp.data.TelehealthSessionRequest;
import com.doctorapp.data.TelehealthSessionResponse;
import com.doctorapp.room.Room;
import com.doctorapp.room.RoomManager;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class UpdateSession {

    @Autowired
    private RoomManager roomManager;


    @PostMapping(value = "/alexa/telehealth/session/update",
        consumes= "application/json", produces = "application/json")
    public TelehealthSessionResponse updateSessionWithOffer(@RequestBody TelehealthSessionRequest updateSession) {
        log.info("Alexa user {} is updating a session {} with an SDP offer {}",
            updateSession.getUserName(), updateSession.getSessionId(), updateSession.getSdpOffer());

        // Find the room
        Room room = roomManager.getRoomOrThrow(updateSession.getSessionId());

        // Create new WebRtcEp for Alexa
        WebRtcEndpoint alexaWebRtcEp = room.createAlexaWebRtcEp(updateSession.getSdpOffer());
        // Generate SDP answer for the updated offer
        String alexaSdpAnswer = alexaWebRtcEp.getLocalSessionDescriptor();
        log.info("Generated updated SDP answer for Alexa: {}", alexaSdpAnswer);

        // Trigger a re-negotiate process on provider side
        WebRtcEndpoint providerWebRtcEp = room.createProviderWebRtcEp(room.getProvider());
        String providerSdpOffer = providerWebRtcEp.generateOffer();
        log.info("Generating a new offer to provider. {}", providerSdpOffer);
        JsonObject updatedSdpOffer = new JsonObject();
        updatedSdpOffer.addProperty("id", "updatedSdpOffer");
        updatedSdpOffer.addProperty("sdpOffer", providerSdpOffer);

        try {
            room.getProvider().sendMessage(updatedSdpOffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        providerWebRtcEp.gatherCandidates();

        room.setUpPipeline();
        return new TelehealthSessionResponse(updateSession.getUserName(), updateSession.getSessionId(), alexaSdpAnswer);
    }
}
