package io.fluentic.ubicdn.extractor.services.soundcloud;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import io.fluentic.ubicdn.extractor.NewPipe;
import io.fluentic.ubicdn.extractor.UrlIdHandler;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;
import io.fluentic.ubicdn.extractor.utils.Parser;
import io.fluentic.ubicdn.extractor.utils.Utils;

public class SoundcloudChannelUrlIdHandler implements UrlIdHandler {

    private static final SoundcloudChannelUrlIdHandler instance = new SoundcloudChannelUrlIdHandler();
    private final String URL_PATTERN = "^https?://(www\\.)?soundcloud.com/[0-9a-z_-]+" +
            "(/((tracks|albums|sets|reposts|followers|following)/?)?)?([#?].*)?$";

    public static SoundcloudChannelUrlIdHandler getInstance() {
        return instance;
    }

    @Override
    public String getUrl(String id) throws ParsingException {
        try {
            return SoundcloudParsingHelper.resolveUrlWithEmbedPlayer("https://api.soundcloud.com/users/" + id);
        } catch (Exception e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    @Override
    public String getId(String url) throws ParsingException {
        Utils.checkUrl(URL_PATTERN, url);

        try {
            return SoundcloudParsingHelper.resolveIdWithEmbedPlayer(url);
        } catch (Exception e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    @Override
    public String cleanUrl(String complexUrl) throws ParsingException {
        Utils.checkUrl(URL_PATTERN, complexUrl);

        try {
            Element ogElement = Jsoup.parse(NewPipe.getDownloader().download(complexUrl))
                    .select("meta[property=og:url]").first();

            return ogElement.attr("content");
        } catch (Exception e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    @Override
    public boolean acceptUrl(String url) {
        return Parser.isMatch(URL_PATTERN, url.toLowerCase());
    }
}
