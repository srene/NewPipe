package io.fluentic.ubicdn.extractor.services.soundcloud;

import java.io.IOException;

import io.fluentic.ubicdn.extractor.StreamingService;
import io.fluentic.ubicdn.extractor.SuggestionExtractor;
import io.fluentic.ubicdn.extractor.UrlIdHandler;
import io.fluentic.ubicdn.extractor.channel.ChannelExtractor;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.kiosk.KioskExtractor;
import io.fluentic.ubicdn.extractor.kiosk.KioskList;
import io.fluentic.ubicdn.extractor.playlist.PlaylistExtractor;
import io.fluentic.ubicdn.extractor.search.SearchEngine;
import io.fluentic.ubicdn.extractor.stream.StreamExtractor;

public class SoundcloudService extends StreamingService {

    public SoundcloudService(int id, String name) {
        super(id, name);
    }

    @Override
    public SearchEngine getSearchEngine() {
        return new SoundcloudSearchEngine(getServiceId());
    }

    @Override
    public UrlIdHandler getStreamUrlIdHandler() {
        return SoundcloudStreamUrlIdHandler.getInstance();
    }

    @Override
    public UrlIdHandler getChannelUrlIdHandler() {
        return SoundcloudChannelUrlIdHandler.getInstance();
    }

    @Override
    public UrlIdHandler getPlaylistUrlIdHandler() {
        return SoundcloudPlaylistUrlIdHandler.getInstance();
    }


    @Override
    public StreamExtractor getStreamExtractor(String url) throws IOException, ExtractionException {
        return new SoundcloudStreamExtractor(this, url);
    }

    @Override
    public ChannelExtractor getChannelExtractor(String url, String nextStreamsUrl) throws IOException, ExtractionException {
        return new SoundcloudChannelExtractor(this, url, nextStreamsUrl);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(String url, String nextStreamsUrl) throws IOException, ExtractionException {
        return new SoundcloudPlaylistExtractor(this, url, nextStreamsUrl);
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return new SoundcloudSuggestionExtractor(getServiceId());
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        KioskList.KioskExtractorFactory chartsFactory = new KioskList.KioskExtractorFactory() {
            @Override
            public KioskExtractor createNewKiosk(StreamingService streamingService,
                                                 String url,
                                                 String nextStreamUrl,
                                                 String id)
                    throws ExtractionException, IOException {
                return new SoundcloudChartsExtractor(SoundcloudService.this,
                        url,
                        nextStreamUrl,
                        id);
            }
        };

        KioskList list = new KioskList(getServiceId());

        // add kiosks here e.g.:
        final SoundcloudChartsUrlIdHandler h = new SoundcloudChartsUrlIdHandler();
        try {
            list.addKioskEntry(chartsFactory, h, "Top 50");
            list.addKioskEntry(chartsFactory, h, "New & hot");
        } catch (Exception e) {
            throw new ExtractionException(e);
        }

        return list;
    }
}
