package com.doctorapp.api;

import com.doctorapp.data.TelehealthSessionRequest;
import com.doctorapp.data.TelehealthSessionResponse;
import com.doctorapp.room.RoomManager;
import com.doctorapp.room.SessionHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class SessionCallEndpoint {

    @Autowired
    private SessionHandler sessionHandler;

    @Autowired
    private RoomManager roomManager;

    @PostMapping(value = "/alexa/telehealth/session/initiate",
        consumes = "application/json", produces = "application/json")
    public TelehealthSessionResponse initiateSessionWithOffer(
        @RequestBody TelehealthSessionRequest initiateSession) {
        log.info("Alexa user {} started a session {} with an SDP offer {}",
            initiateSession.getUserName(), initiateSession.getRoomId(),
            initiateSession.getSdpOffer());

        String alexaSdpAnswer = sessionHandler.initiateSessionHandler(initiateSession, roomManager);
        return new TelehealthSessionResponse(initiateSession.getUserName(),
            initiateSession.getRoomId(), alexaSdpAnswer);
    }

    @PostMapping(value = "/alexa/telehealth/session/update",
        consumes= "application/json", produces = "application/json")
    public TelehealthSessionResponse updateSessionWithOffer(@RequestBody TelehealthSessionRequest updateSession) {
        log.info("Alexa user {} is updating a session {} with an SDP offer {}",
            updateSession.getUserName(), updateSession.getRoomId(), updateSession.getSdpOffer());

        String alexaSdpAnswer = sessionHandler.updateSessionHandler(updateSession, roomManager);
        return new TelehealthSessionResponse(updateSession.getUserName(), updateSession.getRoomId(), alexaSdpAnswer);
    }

    @PostMapping(value = "/alexa/telehealth/session/disconnect",
        consumes= "application/json", produces = "application/json")
    public void disconnectSession(
        @RequestBody TelehealthSessionRequest disconnectSession) {
        log.info("Alexa user {} ended the session {}",
            disconnectSession.getUserName(), disconnectSession.getRoomId());

        sessionHandler.disconnectSessionHandler(disconnectSession, roomManager);
    }
}
