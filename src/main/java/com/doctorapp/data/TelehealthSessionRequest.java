package com.doctorapp.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TelehealthSessionRequest {

    private String userName;

    private String roomId;

    private String sdpOffer;

    private IceServer[] iceServers;

    public String getUserName() {
        return userName;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSdpOffer() {
        return sdpOffer;
    }

    public IceServer[] getIceServers() {
        return iceServers;
    }

    public static class IceServer {
        private String url;
        private String username;
        private String credential;

        public IceServer() {
        }

        public IceServer(String url, String username, String credential) {
            this.url = url;
            this.username = username;
            this.credential = credential;
        }

        public String getUrl() {
            return url;
        }

        public String getUsername() {
            return username;
        }

        public String getCredential() {
            return credential;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }
    }
}