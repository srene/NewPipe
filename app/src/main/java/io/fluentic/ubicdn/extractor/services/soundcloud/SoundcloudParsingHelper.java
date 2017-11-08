package io.fluentic.ubicdn.extractor.services.soundcloud;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import io.fluentic.ubicdn.extractor.Downloader;
import io.fluentic.ubicdn.extractor.NewPipe;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;
import io.fluentic.ubicdn.extractor.exceptions.ReCaptchaException;
import io.fluentic.ubicdn.extractor.stream.StreamInfoItemCollector;
import io.fluentic.ubicdn.extractor.utils.Parser;
import io.fluentic.ubicdn.extractor.utils.Parser.RegexException;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SoundcloudParsingHelper {
    private static String clientId;

    private SoundcloudParsingHelper() {
    }

    public static String clientId() throws ReCaptchaException, IOException, RegexException {
        if (clientId != null && !clientId.isEmpty()) return clientId;

        Downloader dl = NewPipe.getDownloader();

        String response = dl.download("https://soundcloud.com");
        Document doc = Jsoup.parse(response);

        // TODO: Find a less heavy way to get the client_id
        // Currently we are downloading a 1MB file (!) just to get the client_id,
        // youtube-dl don't have a way too, they are just hardcoding and updating it when it becomes invalid.
        // The embed mode has a way to get it, but we still have to download a heavy file (~800KB).
        Element jsElement = doc.select("script[src^=https://a-v2.sndcdn.com/assets/app]").first();
        String js = dl.download(jsElement.attr("src"));

        return clientId = Parser.matchGroup1(",client_id:\"(.*?)\"", js);
    }

    public static String toDateString(String time) throws ParsingException {
        try {
            Date date;
            // Have two date formats, one for the 'api.soundc...' and the other 'api-v2.soundc...'.
            try {
                date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(time);
            } catch (Exception e) {
                date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss +0000").parse(time);
            }

            SimpleDateFormat newDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return newDateFormat.format(date);
        } catch (ParseException e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    /**
     * Call the endpoint "/resolve" of the api.<br/>
     * See https://developers.soundcloud.com/docs/api/reference#resolve
     */
    public static JsonObject resolveFor(String url) throws IOException, ReCaptchaException, ParsingException {
        String apiUrl = "https://api.soundcloud.com/resolve"
                + "?url=" + URLEncoder.encode(url, "UTF-8")
                + "&client_id=" + clientId();

        try {
            return JsonParser.object().from(NewPipe.getDownloader().download(apiUrl));
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }
    }

    /**
     * Fetch the embed player with the apiUrl and return the canonical url (like the permalink_url from the json api).<br/>
     *
     * @return the url resolved
     */
    public static String resolveUrlWithEmbedPlayer(String apiUrl) throws IOException, ReCaptchaException, ParsingException {

        String response = NewPipe.getDownloader().download("https://w.soundcloud.com/player/?url="
                + URLEncoder.encode(apiUrl, "UTF-8"));

        return Jsoup.parse(response).select("link[rel=\"canonical\"]").first().attr("abs:href");
    }

    /**
     * Fetch the embed player with the url and return the id (like the id from the json api).<br/>
     *
     * @return the id resolved
     */
    public static String resolveIdWithEmbedPlayer(String url) throws IOException, ReCaptchaException, ParsingException {

        String response = NewPipe.getDownloader().download("https://w.soundcloud.com/player/?url="
                + URLEncoder.encode(url, "UTF-8"));
        return Parser.matchGroup1(",\"id\":(.*?),", response);
    }

    /**
     * Fetch the streams from the given api and commit each of them to the collector.
     * <p>
     * This differ from {@link #getStreamsFromApi(StreamInfoItemCollector, String)} in the sense that they will always
     * get MIN_ITEMS or more items.
     *
     * @param minItems the method will return only when it have extracted that many items (equal or more)
     */
    public static String getStreamsFromApiMinItems(int minItems, StreamInfoItemCollector collector, String apiUrl) throws IOException, ReCaptchaException, ParsingException {
        String nextStreamsUrl = SoundcloudParsingHelper.getStreamsFromApi(collector, apiUrl);

        while (!nextStreamsUrl.isEmpty() && collector.getItemList().size() < minItems) {
            nextStreamsUrl = SoundcloudParsingHelper.getStreamsFromApi(collector, nextStreamsUrl);
        }

        return nextStreamsUrl;
    }

    /**
     * Fetch the streams from the given api and commit each of them to the collector.
     *
     * @return the next streams url, empty if don't have
     */
    public static String getStreamsFromApi(StreamInfoItemCollector collector, String apiUrl, boolean charts) throws IOException, ReCaptchaException, ParsingException {
        String response = NewPipe.getDownloader().download(apiUrl);
        JsonObject responseObject;
        try {
            responseObject = JsonParser.object().from(response);
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        JsonArray responseCollection = responseObject.getArray("collection");
        for (Object o : responseCollection) {
            if (o instanceof JsonObject) {
                JsonObject object = (JsonObject) o;
                collector.commit(new SoundcloudStreamInfoItemExtractor(charts ? object.getObject("track") : object));
            }
        }

        String nextStreamsUrl;
        try {
            nextStreamsUrl = responseObject.getString("next_href");
            if (!nextStreamsUrl.contains("client_id=")) nextStreamsUrl += "&client_id=" + SoundcloudParsingHelper.clientId();
        } catch (Exception ignored) {
            nextStreamsUrl = "";
        }

        return nextStreamsUrl;
    }

    public static String getStreamsFromApi(StreamInfoItemCollector collector, String apiUrl) throws ReCaptchaException, ParsingException, IOException {
        return getStreamsFromApi(collector, apiUrl, false);
    }
}
