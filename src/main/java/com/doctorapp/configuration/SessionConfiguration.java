package com.doctorapp.configuration;

import com.doctorapp.room.RoomManager;
import com.doctorapp.room.SessionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfiguration {

    @Bean
    public RoomManager roomManager() {
        return new RoomManager();
    }

    @Bean
    public SessionHandler sessionHandler() {
        return new SessionHandler();
    }

}

