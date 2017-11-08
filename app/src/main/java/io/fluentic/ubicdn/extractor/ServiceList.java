package io.fluentic.ubicdn.extractor;

import io.fluentic.ubicdn.extractor.services.soundcloud.SoundcloudService;
import io.fluentic.ubicdn.extractor.services.youtube.YoutubeService;

/**
 * A list of supported services.
 */
public enum ServiceList {
    YouTube(new YoutubeService(0, "YouTube")),
    SoundCloud(new SoundcloudService(1, "SoundCloud"));
//  DailyMotion(new DailyMotionService(2, "DailyMotion"));

    private final StreamingService service;

    ServiceList(StreamingService service) {
        this.service = service;
    }

    public StreamingService getService() {
        return service;
    }

    public StreamingService.ServiceInfo getServiceInfo() {
        return service.getServiceInfo();
    }

    public int getId() {
        return service.getServiceId();
    }

    @Override
    public String toString() {
        return service.getServiceInfo().name;
    }
}
