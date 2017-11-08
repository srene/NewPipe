package io.fluentic.ubicdn.extractor.playlist;

import io.fluentic.ubicdn.extractor.ListExtractor.NextItemsResult;
import io.fluentic.ubicdn.extractor.ListInfo;
import io.fluentic.ubicdn.extractor.NewPipe;
import io.fluentic.ubicdn.extractor.ServiceList;
import io.fluentic.ubicdn.extractor.StreamingService;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;
import io.fluentic.ubicdn.extractor.stream.StreamInfoItemCollector;

import java.io.IOException;
import java.util.ArrayList;

public class PlaylistInfo extends ListInfo {

    public static NextItemsResult getMoreItems(ServiceList serviceItem, String url, String nextStreamsUrl) throws IOException, ExtractionException {
        return getMoreItems(serviceItem.getService(), url, nextStreamsUrl);
    }

    public static NextItemsResult getMoreItems(StreamingService service, String url, String nextStreamsUrl) throws IOException, ExtractionException {
        return service.getPlaylistExtractor(url, nextStreamsUrl).getNextStreams();
    }

    public static PlaylistInfo getInfo(String url) throws IOException, ExtractionException {
        return getInfo(NewPipe.getServiceByUrl(url), url);
    }

    public static PlaylistInfo getInfo(ServiceList serviceItem, String url) throws IOException, ExtractionException {
        return getInfo(serviceItem.getService(), url);
    }

    public static PlaylistInfo getInfo(StreamingService service, String url) throws IOException, ExtractionException {
        return getInfo(service.getPlaylistExtractor(url));
    }

    public static PlaylistInfo getInfo(PlaylistExtractor extractor) throws ParsingException {
        PlaylistInfo info = new PlaylistInfo();

        info.service_id = extractor.getServiceId();
        info.url = extractor.getCleanUrl();
        info.id = extractor.getId();
        info.name = extractor.getName();

        try {
            info.stream_count = extractor.getStreamCount();
        } catch (Exception e) {
            info.errors.add(e);
        }
        try {
            info.thumbnail_url = extractor.getThumbnailUrl();
        } catch (Exception e) {
            info.errors.add(e);
        }
        try {
            info.uploader_url = extractor.getUploaderUrl();
        } catch (Exception e) {
            info.errors.add(e);
        }
        try {
            info.uploader_name = extractor.getUploaderName();
        } catch (Exception e) {
            info.errors.add(e);
        }
        try {
            info.uploader_avatar_url = extractor.getUploaderAvatarUrl();
        } catch (Exception e) {
            info.errors.add(e);
        }
        try {
            info.banner_url = extractor.getBannerUrl();
        } catch (Exception e) {
            info.errors.add(e);
        }
        try {
            StreamInfoItemCollector c = extractor.getStreams();
            info.related_streams = c.getItemList();
            info.errors.addAll(c.getErrors());
        } catch (Exception e) {
            info.errors.add(e);
        }

        // Lists can be null if a exception was thrown during extraction
        if (info.related_streams == null) info.related_streams = new ArrayList<>();

        info.has_more_streams = extractor.hasMoreStreams();
        info.next_streams_url = extractor.getNextStreamsUrl();
        return info;
    }

    public String thumbnail_url;
    public String banner_url;
    public String uploader_url;
    public String uploader_name;
    public String uploader_avatar_url;
    public long stream_count = 0;
}
