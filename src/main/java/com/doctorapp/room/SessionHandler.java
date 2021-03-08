package com.doctorapp.room;

import com.doctorapp.data.TelehealthSessionRequest;
import com.doctorapp.data.TelehealthSessionRequest.IceServer;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.log4j.Log4j2;
import org.kurento.client.WebRtcEndpoint;

@Log4j2
public class SessionHandler {

    public String initiateSessionHandler(TelehealthSessionRequest initiateSession, RoomManager roomManager) {
        // Join room
        Room room = roomManager.getRoomOrCreate(initiateSession.getSessionId());

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
        return alexaSdpAnswer;
    }

    public String updateSessionHandler(TelehealthSessionRequest updateSession, RoomManager roomManager) {
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
        return alexaSdpAnswer;
    }

    public void disconnectSessionHandler(TelehealthSessionRequest disconnectSession, RoomManager roomManager) {
        if (disconnectSession.getSessionId().equals("room")) {
            return;
        }

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
