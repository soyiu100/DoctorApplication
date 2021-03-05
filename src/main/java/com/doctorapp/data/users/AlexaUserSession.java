package com.doctorapp.data.users;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AlexaUserSession extends UserSession {

    public AlexaUserSession(String name, String roomName, String sdpOffer) {
        super(name, roomName);
        this.sdpOffer = sdpOffer;
    }
}
