package com.doctorapp.configuration;

import com.doctorapp.room.RoomManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfiguration {

    @Bean
    public RoomManager roomManager() {
        return new RoomManager();
    }

}

