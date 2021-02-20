package mediaservice.users;

import java.util.ArrayList;
import java.util.List;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;

public class UserSession {

    protected final String name;

    protected String sdpOffer;
    protected String roomName;
    protected WebRtcEndpoint webRtcEndpoint;
    protected final List<IceCandidate> candidateList = new ArrayList<IceCandidate>();

    public UserSession(String name, String roomName) {
        this.name = name;
        this.roomName = roomName;
    }

    public String getName() {
        return name;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getSdpOffer() {
        return sdpOffer;
    }

    public void setSdpOffer(String sdpOffer) {
        this.sdpOffer = sdpOffer;
    }

    public void addCandidate(IceCandidate candidate) {
        if (this.webRtcEndpoint != null) {
            this.webRtcEndpoint.addIceCandidate(candidate);
        } else {
            candidateList.add(candidate);
        }
    }
}