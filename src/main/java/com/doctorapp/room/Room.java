package com.doctorapp.room;

import com.google.gson.JsonObject;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import lombok.extern.log4j.Log4j2;
import com.doctorapp.data.users.WebUserSession;
import org.kurento.client.EventListener;
import org.kurento.client.GStreamerFilter;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;

@Log4j2
public class Room implements Closeable {

    private MediaPipeline pipeline;
    private WebUserSession provider;
    private WebRtcEndpoint providerWebRtcEp;
    private WebRtcEndpoint alexaWebRtcEp;
    private GStreamerFilter flipFilter;
    private GStreamerFilter textOverlayFilter;
    private PlayerEndpoint playerEndpoint;

    private final String roomName;

    public Room(String name, MediaPipeline pipeline) {
        this.roomName = name;
        this.pipeline = pipeline;
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public WebRtcEndpoint getProviderWebRtcEp() {
        return providerWebRtcEp;
    }

    public void setProviderWebRtcEp(WebRtcEndpoint providerWebRtcEp) {
        this.providerWebRtcEp = providerWebRtcEp;
    }

    public WebRtcEndpoint getAlexaWebRtcEp() {
        return alexaWebRtcEp;
    }

    public void setAlexaWebRtcEp(WebRtcEndpoint alexaWebRtcEp) {
        this.alexaWebRtcEp = alexaWebRtcEp;
    }

    public String getRoomName() {
        return roomName;
    }

    public void joinAsProvider(String userName, WebUserSession userSession) {
        this.provider = userSession;
        log.info("Provider {} has joined", userName);
    }

    public WebUserSession getProvider() {
        return provider;
    }

    public void setUpPipeline() {
        // Provider -> flipFilter -> textOverlayFilter -> Alexa
        // Provider <------------------------------------ Alexa
        flipFilter = new GStreamerFilter
            .Builder(pipeline, "videoflip method=horizontal-flip").build();
        textOverlayFilter = new GStreamerFilter
            .Builder(pipeline, "textoverlay font-desc=\"Sans 24\" text="
            + "\"" + provider.getName() + "\""
            + " valignment=top halignment=left").build();
        alexaWebRtcEp.connect(providerWebRtcEp);
        providerWebRtcEp.connect(flipFilter);
        flipFilter.connect(textOverlayFilter);
        textOverlayFilter.connect(alexaWebRtcEp);
        log.info("Provider have been connected with Alexa");
    }

    public WebRtcEndpoint createProviderWebRtcEp(WebUserSession provider) {
        WebRtcEndpoint providerWebRtcEp = new WebRtcEndpoint.Builder(pipeline).build();
        setProviderWebRtcEp(providerWebRtcEp);
        if (alexaWebRtcEp == null) {
            buildWelcomeConnection(providerWebRtcEp);
        }

        providerWebRtcEp.addIceCandidateFoundListener(
            new EventListener<IceCandidateFoundEvent>() {
                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));

                    try {
                        // Send KMS's ice candidate to local peer
                        provider.sendMessage(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        return providerWebRtcEp;
    }

    public WebRtcEndpoint createAlexaWebRtcEp(String sdpOffer) {
        WebRtcEndpoint alexaWebRtcEp = new WebRtcEndpoint.Builder(pipeline).useDataChannels().build();
        setAlexaWebRtcEp(alexaWebRtcEp);
        if (providerWebRtcEp == null) {
            buildWelcomeConnection(alexaWebRtcEp);
        }

        String alexaSdpAnswer = alexaWebRtcEp.processOffer(sdpOffer);
        final CountDownLatch latch = new CountDownLatch(1);
        alexaWebRtcEp.addOnIceGatheringDoneListener(event -> {
            latch.countDown();
        });

        alexaWebRtcEp.gatherCandidates();
        try {
            latch.await();
        } catch (InterruptedException e) {
            // Should not reach here
        }
        return alexaWebRtcEp;
    }

    // Provider is released
    // PlayerEndpoint ---> Alexa
    public void disconnectProvider() {
        if (alexaWebRtcEp != null) {
            alexaWebRtcEp.disconnect(providerWebRtcEp);
            providerWebRtcEp.disconnect(flipFilter);
        }
        providerWebRtcEp.release();
        providerWebRtcEp = null;
        log.info("Provider has been disconnected");
    }

    // Alexa is released
    // PlayerEndpoint ---> Provider
    public void disconnectAlexa() {
        if (providerWebRtcEp != null) {
            providerWebRtcEp.disconnect(flipFilter);
            flipFilter.disconnect(textOverlayFilter);
            textOverlayFilter.disconnect(alexaWebRtcEp);
            alexaWebRtcEp.disconnect(providerWebRtcEp);
        }
        alexaWebRtcEp.release();
        alexaWebRtcEp = null;
        log.info("Patient has been disconnected");
    }

    private void releaseFilters() {
        if (flipFilter != null) {
            flipFilter.release();
        }

        if (textOverlayFilter != null) {
            textOverlayFilter.release();
        }
    }

    public void buildWelcomeConnection(WebRtcEndpoint webRtcEndpoint) {
        playerEndpoint = new PlayerEndpoint.Builder(pipeline,
            "https://mumu-public.s3-us-west-2.amazonaws.com/nationwide-720p-trimmed.mp4").build();
        playerEndpoint.connect(webRtcEndpoint);
        playerEndpoint.play();
        log.info("PlayerEndpoint has been connected and started playing");
    }

    public void disconnectWelcomeConnection(WebRtcEndpoint webRtcEndpoint) {
        playerEndpoint.disconnect(webRtcEndpoint);
        playerEndpoint.release();
        playerEndpoint = null;
        log.info("PlayerEndpoint has been released");
    }

    @Override
    public void close() {
        releaseFilters();
        provider = null;
        pipeline.release();
        pipeline = null;

        log.info("Room {} closed", this.roomName);
    }
}
