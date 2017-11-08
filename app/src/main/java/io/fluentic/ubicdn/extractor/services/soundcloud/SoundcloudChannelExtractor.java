package io.fluentic.ubicdn.extractor.services.soundcloud;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import io.fluentic.ubicdn.extractor.Downloader;
import io.fluentic.ubicdn.extractor.NewPipe;
import io.fluentic.ubicdn.extractor.StreamingService;
import io.fluentic.ubicdn.extractor.channel.ChannelExtractor;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;
import io.fluentic.ubicdn.extractor.stream.StreamInfoItemCollector;

import java.io.IOException;

@SuppressWarnings("WeakerAccess")
public class SoundcloudChannelExtractor extends ChannelExtractor {
    private String userId;
    private JsonObject user;

    public SoundcloudChannelExtractor(StreamingService service, String url, String nextStreamsUrl) throws IOException, ExtractionException {
        super(service, url, nextStreamsUrl);
    }

    @Override
    public void fetchPage() throws IOException, ExtractionException {
        Downloader dl = NewPipe.getDownloader();

        userId = getUrlIdHandler().getId(getOriginalUrl());
        String apiUrl = "https://api.soundcloud.com/users/" + userId +
                "?client_id=" + SoundcloudParsingHelper.clientId();

        String response = dl.download(apiUrl);
        try {
            user = JsonParser.object().from(response);
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }
    }

    @Override
    public String getCleanUrl() {
        return user.isString("permalink_url") ? user.getString("permalink_url") : getOriginalUrl();
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public String getName() {
        return user.getString("username");
    }

    @Override
    public String getAvatarUrl() {
        return user.getString("avatar_url");
    }

    @Override
    public String getBannerUrl() {
        try {
            return user.getObject("visuals").getArray("visuals").getObject(0).getString("visual_url", "");
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public String getFeedUrl() {
        return null;
    }

    @Override
    public long getSubscriberCount() {
        return user.getNumber("followers_count", 0).longValue();
    }

    @Override
    public String getDescription() throws ParsingException {
        return user.getString("description", "");
    }

    @Override
    public StreamInfoItemCollector getStreams() throws IOException, ExtractionException {
        StreamInfoItemCollector collector = new StreamInfoItemCollector(getServiceId());

        String apiUrl = "https://api-v2.soundcloud.com/users/" + getId() + "/tracks"
                + "?client_id=" + SoundcloudParsingHelper.clientId()
                + "&limit=20"
                + "&linked_partitioning=1";

        nextStreamsUrl = SoundcloudParsingHelper.getStreamsFromApiMinItems(15, collector, apiUrl);
        return collector;
    }

    @Override
    public NextItemsResult getNextStreams() throws IOException, ExtractionException {
        if (!hasMoreStreams()) {
            throw new ExtractionException("Channel doesn't have more streams");
        }

        StreamInfoItemCollector collector = new StreamInfoItemCollector(getServiceId());
        nextStreamsUrl = SoundcloudParsingHelper.getStreamsFromApiMinItems(15, collector, nextStreamsUrl);

        return new NextItemsResult(collector, nextStreamsUrl);
    }
}
