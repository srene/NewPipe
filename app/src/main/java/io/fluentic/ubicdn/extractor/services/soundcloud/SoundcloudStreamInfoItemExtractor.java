package io.fluentic.ubicdn.extractor.services.soundcloud;

import com.grack.nanojson.JsonObject;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;
import io.fluentic.ubicdn.extractor.stream.StreamInfoItemExtractor;
import io.fluentic.ubicdn.extractor.stream.StreamType;

public class SoundcloudStreamInfoItemExtractor implements StreamInfoItemExtractor {

    protected final JsonObject searchResult;

    public SoundcloudStreamInfoItemExtractor(JsonObject searchResult) {
        this.searchResult = searchResult;
    }

    @Override
    public String getUrl() {
        return searchResult.getString("permalink_url");
    }

    @Override
    public String getName() {
        return searchResult.getString("title");
    }

    @Override
    public long getDuration() {
        return searchResult.getNumber("duration", 0).longValue() / 1000L;
    }

    @Override
    public String getUploaderName() {
        return searchResult.getObject("user").getString("username");
    }

    @Override
    public String getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.toDateString(searchResult.getString("created_at"));
    }

    @Override
    public long getViewCount() {
        return searchResult.getNumber("playback_count", 0).longValue();
    }

    @Override
    public String getThumbnailUrl() {
        return searchResult.getString("artwork_url");
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO_STREAM;
    }

    @Override
    public boolean isAd() {
        return false;
    }
}
