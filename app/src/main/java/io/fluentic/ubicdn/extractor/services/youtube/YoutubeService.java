package io.fluentic.ubicdn.extractor.services.youtube;

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

import java.io.IOException;


/*
 * Created by Christian Schabesberger on 23.08.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * YoutubeService.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class YoutubeService extends StreamingService {

    public YoutubeService(int id, String name) {
        super(id, name);
    }

    @Override
    public SearchEngine getSearchEngine() {
        return new YoutubeSearchEngine(getServiceId());
    }

    @Override
    public UrlIdHandler getStreamUrlIdHandler() {
        return YoutubeStreamUrlIdHandler.getInstance();
    }

    @Override
    public UrlIdHandler getChannelUrlIdHandler() {
        return YoutubeChannelUrlIdHandler.getInstance();
    }

    @Override
    public UrlIdHandler getPlaylistUrlIdHandler() {
        return YoutubePlaylistUrlIdHandler.getInstance();
    }


    @Override
    public StreamExtractor getStreamExtractor(String url) throws IOException, ExtractionException {
        return new YoutubeStreamExtractor(this, url);
    }

    @Override
    public ChannelExtractor getChannelExtractor(String url, String nextStreamsUrl) throws IOException, ExtractionException {
        return new YoutubeChannelExtractor(this, url, nextStreamsUrl);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(String url, String nextStreamsUrl) throws IOException, ExtractionException {
        return new YoutubePlaylistExtractor(this, url, nextStreamsUrl);
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return new YoutubeSuggestionExtractor(getServiceId());
    }

    @Override
    public KioskList getKioskList()
            throws ExtractionException {
        KioskList list = new KioskList(getServiceId());

        // add kiosks here e.g.:
        try {
            list.addKioskEntry(new KioskList.KioskExtractorFactory() {
                @Override
                public KioskExtractor createNewKiosk(StreamingService streamingService, String url, String nextStreamUrl, String id)
                throws ExtractionException, IOException {
                    return new YoutubeTrendingExtractor(YoutubeService.this, url, nextStreamUrl, id);
                }
            }, new YoutubeTrendingUrlIdHandler(), "Trending");
            list.setDefaultKiosk("Trending");
        } catch (Exception e) {
            throw new ExtractionException(e);
        }

        return list;
    }
}
