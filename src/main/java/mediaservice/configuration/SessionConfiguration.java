package mediaservice.configuration;

import mediaservice.room.RoomManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfiguration {

    @Bean
    public RoomManager roomManager() {
        return new RoomManager();
    }

}

