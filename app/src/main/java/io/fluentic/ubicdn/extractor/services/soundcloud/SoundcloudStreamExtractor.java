package io.fluentic.ubicdn.extractor.services.soundcloud;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import io.fluentic.ubicdn.extractor.Downloader;
import io.fluentic.ubicdn.extractor.MediaFormat;
import io.fluentic.ubicdn.extractor.NewPipe;
import io.fluentic.ubicdn.extractor.StreamingService;
import io.fluentic.ubicdn.extractor.exceptions.ContentNotAvailableException;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;
import io.fluentic.ubicdn.extractor.stream.*;
import io.fluentic.ubicdn.extractor.utils.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoundcloudStreamExtractor extends StreamExtractor {
    private JsonObject track;

    public SoundcloudStreamExtractor(StreamingService service, String url) throws IOException, ExtractionException {
        super(service, url);
    }

    @Override
    public void fetchPage() throws IOException, ExtractionException {
        track = SoundcloudParsingHelper.resolveFor(getOriginalUrl());

        String policy = track.getString("policy", "");
        if (!policy.equals("ALLOW") && !policy.equals("MONETIZE")) {
            throw new ContentNotAvailableException("Content not available: policy " + policy);
        }
    }

    @Override
    public String getCleanUrl() {
        return track.isString("permalink_url") ? track.getString("permalink_url") : getOriginalUrl();
    }

    @Override
    public String getId() {
        return track.getInt("id") + "";
    }

    @Override
    public String getName() {
        return track.getString("title");
    }

    @Override
    public String getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.toDateString(track.getString("created_at"));
    }

    @Override
    public String getThumbnailUrl() {
        return track.getString("artwork_url", "");
    }

    @Override
    public String getDescription() {
        return track.getString("description");
    }

    @Override
    public int getAgeLimit() {
        return 0;
    }

    @Override
    public long getLength() {
        return track.getNumber("duration", 0).longValue() / 1000L;
    }

    @Override
    public long getTimeStamp() throws ParsingException {
        String timeStamp;
        try {
            timeStamp = Parser.matchGroup1("(#t=\\d{0,3}h?\\d{0,3}m?\\d{1,3}s?)", getOriginalUrl());
        } catch (Parser.RegexException e) {
            // catch this instantly since an url does not necessarily have to have a time stamp

            // -2 because well the testing system will then know its the regex that failed :/
            // not good i know
            return -2;
        }

        if (!timeStamp.isEmpty()) {
            try {
                String secondsString = "";
                String minutesString = "";
                String hoursString = "";
                try {
                    secondsString = Parser.matchGroup1("(\\d{1,3})s", timeStamp);
                    minutesString = Parser.matchGroup1("(\\d{1,3})m", timeStamp);
                    hoursString = Parser.matchGroup1("(\\d{1,3})h", timeStamp);
                } catch (Exception e) {
                    //it could be that time is given in another method
                    if (secondsString.isEmpty() //if nothing was got,
                            && minutesString.isEmpty()//treat as unlabelled seconds
                            && hoursString.isEmpty()) {
                        secondsString = Parser.matchGroup1("t=(\\d+)", timeStamp);
                    }
                }

                int seconds = secondsString.isEmpty() ? 0 : Integer.parseInt(secondsString);
                int minutes = minutesString.isEmpty() ? 0 : Integer.parseInt(minutesString);
                int hours = hoursString.isEmpty() ? 0 : Integer.parseInt(hoursString);

                //don't trust BODMAS!
                return seconds + (60 * minutes) + (3600 * hours);
                //Log.d(TAG, "derived timestamp value:"+ret);
                //the ordering varies internationally
            } catch (ParsingException e) {
                throw new ParsingException("Could not get timestamp.", e);
            }
        } else {
            return 0;
        }
    }

    @Override
    public long getViewCount() {
        return track.getNumber("playback_count", 0).longValue();
    }

    @Override
    public long getLikeCount() {
        return track.getNumber("favoritings_count", -1).longValue();
    }

    @Override
    public long getDislikeCount() {
        return -1;
    }

    @Override
    public String getUploaderUrl() {
        return track.getObject("user").getString("permalink_url", "");
    }

    @Override
    public String getUploaderName() {
        return track.getObject("user").getString("username", "");
    }

    @Override
    public String getUploaderAvatarUrl() {
        return track.getObject("user", new JsonObject()).getString("avatar_url", "");
    }

    @Override
    public String getDashMpdUrl() {
        return null;
    }

    @Override
    public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        List<AudioStream> audioStreams = new ArrayList<>();
        Downloader dl = NewPipe.getDownloader();

        String apiUrl = "https://api.soundcloud.com/i1/tracks/" + getId() + "/streams"
                + "?client_id=" + SoundcloudParsingHelper.clientId();

        String response = dl.download(apiUrl);
        JsonObject responseObject;
        try {
            responseObject = JsonParser.object().from(response);
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        String mp3Url = responseObject.getString("http_mp3_128_url");
        if (mp3Url != null && !mp3Url.isEmpty()) {
            audioStreams.add(new AudioStream(mp3Url, MediaFormat.MP3.id, 128));
        } else {
            throw new ExtractionException("Could not get SoundCloud's track audio url");
        }

        return audioStreams;
    }

    @Override
    public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        return null;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() throws IOException, ExtractionException {
        return null;
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO_STREAM;
    }

    @Override
    public StreamInfoItem getNextVideo() throws IOException, ExtractionException {
        return null;
    }

    @Override
    public StreamInfoItemCollector getRelatedVideos() throws IOException, ExtractionException {
        StreamInfoItemCollector collector = new StreamInfoItemCollector(getServiceId());

        String apiUrl = "https://api-v2.soundcloud.com/tracks/" + getId() + "/related"
                + "?client_id=" + SoundcloudParsingHelper.clientId();

        SoundcloudParsingHelper.getStreamsFromApi(collector, apiUrl);
        return collector;
    }


    @Override
    public String getErrorMessage() {
        return null;
    }
}
