package com.doctorapp.api;

import com.doctorapp.data.users.AlexaUserSession;
import com.doctorapp.data.users.UserRegistry;
import com.doctorapp.room.Room;
import com.doctorapp.room.RoomManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.log4j.Log4j2;
import com.doctorapp.data.TelehealthSessionRequest;
import com.doctorapp.data.TelehealthSessionRequest.IceServer;
import com.doctorapp.data.TelehealthSessionResponse;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class InitiateSession {

    @Autowired
    private UserRegistry registry;

    @Autowired
    private RoomManager roomManager;

    @PostMapping(value = "/alexa/telehealth/session/initiate",
        consumes = "application/json", produces = "application/json")
    public TelehealthSessionResponse initiateSessionWithOffer(
        @RequestBody TelehealthSessionRequest initiateSession) {
        log.info("Alexa user {} started a session {} with an SDP offer {}",
            initiateSession.getUserName(), initiateSession.getSessionId(),
            initiateSession.getSdpOffer());

        // Check appointment schedule

        // Write into TelehealthSessionTable

        // Write into MeetingRoom table and get room name
//		meetingRoomDAO.addPatientToMeetingRoom(initiateSession.getUserName(), initiateSession.getSessionId());

        // Register Alexa user
        AlexaUserSession alexaUserSession = new AlexaUserSession(initiateSession.getUserName(),
            initiateSession.getSessionId(),
            initiateSession.getSdpOffer());
        log.info("alexa user room is {}", alexaUserSession.getRoomName());
        registry.registerAlexaUser(alexaUserSession);
        log.info("Alexa user {} has been registered successfully", initiateSession.getUserName());

        // Join room
        Room room = roomManager.getRoomOrCreate(initiateSession.getSessionId());
        room.joinAsAlexa(initiateSession.getUserName(), alexaUserSession);

        // start the call
        if (initiateSession.getIceServers() != null) {
            setIceServers(initiateSession.getIceServers(), room.getProviderWebRtcEp());
        }

        // Generate SDP answer for callee (Alexa) and return
        log.info("Generating SDP answer for Alexa");
        WebRtcEndpoint alexaWebRtcEp = room.createAlexaWebRtcEp(initiateSession.getSdpOffer());
        if (room.getProviderWebRtcEp() != null) {
            room.disconnectWelcomeConnection(room.getProviderWebRtcEp());
            room.setUpPipeline();
        }

        String alexaSdpAnswer = alexaWebRtcEp.getLocalSessionDescriptor();
        log.info("Answer generated for Alexa: {} ", alexaSdpAnswer);
        return new TelehealthSessionResponse(initiateSession.getUserName(),
            initiateSession.getSessionId(), alexaSdpAnswer);
    }

    private void setIceServers(IceServer[] iceServers, WebRtcEndpoint providerWebRtcEp) {
        try {
            for (IceServer iceServer : iceServers) {
                if (iceServer.getUrl().startsWith("stun")) {
                    //url: stun:mrs-2a687c05-1.p.us-east-1.cmds-tachyon.com:4172
                    String stunUrl = iceServer.getUrl();
                    String[] stun = stunUrl.split(":");
                    InetAddress inetAddress = InetAddress.getByName(stun[1]);
                    providerWebRtcEp.setStunServerAddress(inetAddress.getHostAddress());
                    log.info("Stun: {} {}", inetAddress.getHostAddress(), stun[2]);
                    providerWebRtcEp.setStunServerPort(Integer.parseInt(stun[2]));
                } else {
                    // url: turns:mrs-2a687c05-1.p.us-east-1.cmds-tachyon.com:443?transport=tcp
                    // username: 1605127832:tk9fafcdbf-404c-4bda-96d2-402dd262fc0a-us-east-1_1605122432579_0
                    // credential: qYmEjPp03svJDe66GWG4v2q2fGk=
                    String[] turn = iceServer.getUrl().split(":");
                    InetAddress inetAddress = InetAddress.getByName(turn[1]);
                    String newTurnUrl =
                        iceServer.getUsername() + ":" + iceServer.getCredential() + "@"
                            + inetAddress.getHostAddress() + ":" + turn[2];
                    log.info("Turn url: {}", newTurnUrl);
                    providerWebRtcEp.setTurnUrl(newTurnUrl);
                }
            }
        } catch (UnknownHostException e) {
            log.error("UnknownHostException", e);
        }
    }
}
