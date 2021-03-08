package com.doctorapp.room;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.log4j.Log4j2;
import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Log4j2
public class RoomManager {

    @Autowired
    private KurentoClient kurento;

    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    /**
     * Looks for a room in the active room list.
     *
     * @param roomName the name of the room
     * @return the room if it was already created, or a new one if it is the first time this room is
     * accessed
     */
    public Room getRoomOrCreate(String roomName) {
        log.info("Searching for room {}", roomName);
        Room room = rooms.get(roomName);

        if (room == null) {
            log.info("Room {} not exist. Will create now!", roomName);
            room = new Room(roomName, kurento.createMediaPipeline());
            rooms.put(roomName, room);
        }
        log.info("Room {} found!", roomName);
        return room;
    }

    public Room getRoomOrThrow(String roomName) {
        log.info("Searching for room {}", roomName);
        Room room = rooms.get(roomName);

        if (room == null) {
            throw new IllegalStateException(String.format("The session %s doesn't exist anymore.", roomName));
        }
        log.info("Room {} found!", roomName);
        return room;
    }

    /**
     * Removes a room from the list of available rooms.
     *
     * @param room the room to be removed
     */
    public void removeRoom(Room room) {
        room.close();
        rooms.remove(room.getRoomName());
        log.info("Room {} removed and closed", room.getRoomName());
    }

}