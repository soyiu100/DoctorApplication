package com.doctorapp.data.users;

import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.WebSocketSession;

/**
 * Map of users registered in the system. This class has a concurrent hash map to store users, using
 * its name as key in the map.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.3.1
 */
@Log4j2
public class UserRegistry {

    private ConcurrentHashMap<String, UserSession> usersByName = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, UserSession> usersBySessionId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, WebSocketSession> wsSessionByName = new ConcurrentHashMap<>();

    public void registerAlexaUser(AlexaUserSession user) {
        usersByName.put(user.getName(), user);
    }

    public void registerWebUser(WebUserSession user) {
        usersByName.put(user.getName(), user);
        usersBySessionId.put(user.getSession().getId(), user);
        wsSessionByName.put(user.getName(), user.getSession());
        log.info("User {} in UserRegistry", user.getName());
    }

    public UserSession getBySession(WebSocketSession session) {
        return usersBySessionId.get(session.getId());
    }

    public WebSocketSession getWSSessionByName(String name) {
        return wsSessionByName.get(name);
    }

    public UserSession removeBySession(WebSocketSession session) {
        final UserSession user = getBySession(session);
        if (user != null) {
            usersByName.remove(user.getName());
            usersBySessionId.remove(session.getId());
            wsSessionByName.remove(session.getId());
        }
        return user;
    }

}