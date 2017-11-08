package io.fluentic.ubicdn.extractor;

import io.fluentic.ubicdn.extractor.channel.ChannelExtractor;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.kiosk.KioskList;
import io.fluentic.ubicdn.extractor.playlist.PlaylistExtractor;
import io.fluentic.ubicdn.extractor.search.SearchEngine;
import io.fluentic.ubicdn.extractor.stream.StreamExtractor;

import java.io.IOException;

public abstract class StreamingService {
    public class ServiceInfo {
        public final String name;

        public ServiceInfo(String name) {
            this.name = name;
        }
    }

    public enum LinkType {
        NONE,
        STREAM,
        CHANNEL,
        PLAYLIST
    }

    private final int serviceId;
    private final ServiceInfo serviceInfo;

    public StreamingService(int id, String name) {
        this.serviceId = id;
        this.serviceInfo = new ServiceInfo(name);
    }

    public final int getServiceId() {
        return serviceId;
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public abstract UrlIdHandler getStreamUrlIdHandler();
    public abstract UrlIdHandler getChannelUrlIdHandler();
    public abstract UrlIdHandler getPlaylistUrlIdHandler();
    public abstract SearchEngine getSearchEngine();
    public abstract SuggestionExtractor getSuggestionExtractor();
    public abstract StreamExtractor getStreamExtractor(String url) throws IOException, ExtractionException;
    public abstract ChannelExtractor getChannelExtractor(String url, String nextStreamsUrl) throws IOException, ExtractionException;
    public abstract PlaylistExtractor getPlaylistExtractor(String url, String nextStreamsUrl) throws IOException, ExtractionException;
    public abstract KioskList getKioskList() throws ExtractionException;

    public ChannelExtractor getChannelExtractor(String url) throws IOException, ExtractionException {
        return getChannelExtractor(url, null);
    }

    public PlaylistExtractor getPlaylistExtractor(String url) throws IOException, ExtractionException {
        return getPlaylistExtractor(url, null);
    }

    /**
     * figure out where the link is pointing to (a channel, video, playlist, etc.)
     */
    public final LinkType getLinkTypeByUrl(String url) {
        UrlIdHandler sH = getStreamUrlIdHandler();
        UrlIdHandler cH = getChannelUrlIdHandler();
        UrlIdHandler pH = getPlaylistUrlIdHandler();

        if (sH.acceptUrl(url)) {
            return LinkType.STREAM;
        } else if (cH.acceptUrl(url)) {
            return LinkType.CHANNEL;
        } else if (pH.acceptUrl(url)) {
            return LinkType.PLAYLIST;
        } else {
            return LinkType.NONE;
        }
    }
}
