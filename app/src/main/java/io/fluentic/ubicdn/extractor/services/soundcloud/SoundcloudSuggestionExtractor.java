package io.fluentic.ubicdn.extractor.services.soundcloud;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import io.fluentic.ubicdn.extractor.Downloader;
import io.fluentic.ubicdn.extractor.NewPipe;
import io.fluentic.ubicdn.extractor.SuggestionExtractor;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SoundcloudSuggestionExtractor extends SuggestionExtractor {

    public static final String CHARSET_UTF_8 = "UTF-8";

    public SoundcloudSuggestionExtractor(int serviceId) {
        super(serviceId);
    }

    @Override
    public List<String> suggestionList(String query, String contentCountry) throws IOException, ExtractionException {
        List<String> suggestions = new ArrayList<>();

        Downloader dl = NewPipe.getDownloader();

        String url = "https://api-v2.soundcloud.com/search/queries"
                + "?q=" + URLEncoder.encode(query, CHARSET_UTF_8)
                + "&client_id=" + SoundcloudParsingHelper.clientId()
                + "&limit=10";

        String response = dl.download(url);
        try {
            JsonArray collection = JsonParser.object().from(response).getArray("collection");
            for (Object suggestion : collection) {
                if (suggestion instanceof JsonObject) suggestions.add(((JsonObject) suggestion).getString("query"));
            }

            return suggestions;
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }
    }
}
