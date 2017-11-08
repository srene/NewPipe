package io.fluentic.ubicdn.extractor.playlist;

import io.fluentic.ubicdn.extractor.ListExtractor;
import io.fluentic.ubicdn.extractor.StreamingService;
import io.fluentic.ubicdn.extractor.UrlIdHandler;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;

import java.io.IOException;

public abstract class PlaylistExtractor extends ListExtractor {

    public PlaylistExtractor(StreamingService service, String url, String nextStreamsUrl) throws IOException, ExtractionException {
        super(service, url, nextStreamsUrl);
    }

    @Override
    protected UrlIdHandler getUrlIdHandler() throws ParsingException {
        return getService().getPlaylistUrlIdHandler();
    }

    public abstract String getThumbnailUrl() throws ParsingException;
    public abstract String getBannerUrl() throws ParsingException;

    public abstract String getUploaderUrl() throws ParsingException;
    public abstract String getUploaderName() throws ParsingException;
    public abstract String getUploaderAvatarUrl() throws ParsingException;

    public abstract long getStreamCount() throws ParsingException;
}
