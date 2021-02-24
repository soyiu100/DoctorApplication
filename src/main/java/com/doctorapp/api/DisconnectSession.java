package com.doctorapp.api;

import lombok.extern.log4j.Log4j2;
import com.doctorapp.data.TelehealthSessionRequest;
import com.doctorapp.room.Room;
import com.doctorapp.room.RoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class DisconnectSession {

    @Autowired
    private RoomManager roomManager;

    @PostMapping(value = "/alexa/telehealth/session/disconnect",
        consumes= "application/json", produces = "application/json")
    public void disconnectSession(
        @RequestBody TelehealthSessionRequest disconnectSession) {
        log.info("Alexa user {} ended the session {}",
            disconnectSession.getUserName(), disconnectSession.getSessionId());
        Room room = roomManager.getRoomOrThrow(disconnectSession.getSessionId());

        room.disconnectAlexa();
        // Show waiting room if provider not null
        if (room.getProviderWebRtcEp() != null) {
            room.buildWelcomeConnection(room.getProviderWebRtcEp());
        }

        if (room.getProviderWebRtcEp() == null && room.getAlexaWebRtcEp() == null) {
            roomManager.removeRoom(room);
        }
    }
}