package io.fluentic.ubicdn.extractor.services.youtube;


import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import io.fluentic.ubicdn.extractor.Downloader;
import io.fluentic.ubicdn.extractor.NewPipe;
import io.fluentic.ubicdn.extractor.StreamingService;
import io.fluentic.ubicdn.extractor.channel.ChannelExtractor;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;
import io.fluentic.ubicdn.extractor.exceptions.ReCaptchaException;
import io.fluentic.ubicdn.extractor.stream.StreamInfoItemCollector;
import io.fluentic.ubicdn.extractor.utils.Parser;
import io.fluentic.ubicdn.extractor.utils.Utils;

import java.io.IOException;

/*
 * Created by Christian Schabesberger on 25.07.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * YoutubeChannelExtractor.java is part of NewPipe.
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

@SuppressWarnings("WeakerAccess")
public class YoutubeChannelExtractor extends ChannelExtractor {
    private static final String CHANNEL_FEED_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=";
    private static final String CHANNEL_URL_PARAMETERS = "/videos?view=0&flow=list&sort=dd&live_view=10000";

    private Document doc;
    /**
     * It's lazily initialized (when getNextStreams is called)
     */
    private Document nextStreamsAjax;

    private boolean fetchingNextStreams;

    public YoutubeChannelExtractor(StreamingService service, String url, String nextStreamsUrl) throws IOException, ExtractionException {
        super(service, url, nextStreamsUrl);
    }

    @Override
    public void fetchPage() throws IOException, ExtractionException {
        Downloader downloader = NewPipe.getDownloader();

        String channelUrl = super.getCleanUrl() + CHANNEL_URL_PARAMETERS;
        String pageContent = downloader.download(channelUrl);
        doc = Jsoup.parse(pageContent, channelUrl);

        if (!fetchingNextStreams) {
            nextStreamsUrl = getNextStreamsUrlFrom(doc);
        }
        nextStreamsAjax = null;
    }

    @Override
    protected boolean fetchPageUponCreation() {
        // Unfortunately, we have to fetch the page even if we are getting only next streams,
        // as they don't deliver enough information on their own (the channel name, for example).
        fetchingNextStreams = nextStreamsUrl != null && !nextStreamsUrl.isEmpty();
        return true;
    }

    @Override
    public String getCleanUrl() {
        try {
            return "https://www.youtube.com/channel/" + getId();
        } catch (ParsingException e) {
            return super.getCleanUrl();
        }
    }

    @Override
    public String getId() throws ParsingException {
        try {
            Element element = doc.getElementsByClass("yt-uix-subscription-button").first();
            if (element == null) element = doc.getElementsByClass("yt-uix-subscription-preferences-button").first();

            return element.attr("data-channel-external-id");
        } catch (Exception e) {
            throw new ParsingException("Could not get channel id", e);
        }
    }

    @Override
    public String getName() throws ParsingException {
        try {
            return doc.select("meta[property=\"og:title\"]").first().attr("content");
        } catch (Exception e) {
            throw new ParsingException("Could not get channel name", e);
        }
    }

    @Override
    public String getAvatarUrl() throws ParsingException {
        try {
            return doc.select("img[class=\"channel-header-profile-image\"]").first().attr("abs:src");
        } catch (Exception e) {
            throw new ParsingException("Could not get avatar", e);
        }
    }

    @Override
    public String getBannerUrl() throws ParsingException {
        try {
            Element el = doc.select("div[id=\"gh-banner\"]").first().select("style").first();
            String cssContent = el.html();
            String url = "https:" + Parser.matchGroup1("url\\(([^)]+)\\)", cssContent);

            return url.contains("s.ytimg.com") || url.contains("default_banner") ? null : url;
        } catch (Exception e) {
            throw new ParsingException("Could not get Banner", e);
        }
    }

    @Override
    public String getFeedUrl() throws ParsingException {
        try {
            return CHANNEL_FEED_BASE + getId();
        } catch (Exception e) {
            throw new ParsingException("Could not get feed url", e);
        }
    }

    @Override
    public long getSubscriberCount() throws ParsingException {
        Element el = doc.select("span[class*=\"yt-subscription-button-subscriber-count\"]").first();
        if (el != null) {
            return Long.parseLong(Utils.removeNonDigitCharacters(el.text()));
        } else {
            throw new ParsingException("Could not get subscriber count");
        }
    }

    @Override
    public String getDescription() throws ParsingException {
        try {
            return doc.select("meta[name=\"description\"]").first().attr("content");
        } catch (Exception e) {
            throw new ParsingException("Could not get channel description", e);
        }
    }

    @Override
    public StreamInfoItemCollector getStreams() throws IOException, ExtractionException {
        StreamInfoItemCollector collector = new StreamInfoItemCollector(getServiceId());
        Element ul = doc.select("ul[id=\"browse-items-primary\"]").first();
        collectStreamsFrom(collector, ul);
        return collector;
    }

    @Override
    public NextItemsResult getNextStreams() throws IOException, ExtractionException {
        if (!hasMoreStreams()) {
            throw new ExtractionException("Channel doesn't have more streams");
        }

        StreamInfoItemCollector collector = new StreamInfoItemCollector(getServiceId());
        setupNextStreamsAjax(NewPipe.getDownloader());
        collectStreamsFrom(collector, nextStreamsAjax.select("body").first());

        return new NextItemsResult(collector, nextStreamsUrl);
    }

    private void setupNextStreamsAjax(Downloader downloader) throws IOException, ReCaptchaException, ParsingException {
        String ajaxDataRaw = downloader.download(nextStreamsUrl);
        try {
            JsonObject ajaxData = JsonParser.object().from(ajaxDataRaw);

            String htmlDataRaw = ajaxData.getString("content_html");
            nextStreamsAjax = Jsoup.parse(htmlDataRaw, nextStreamsUrl);

            String nextStreamsHtmlDataRaw = ajaxData.getString("load_more_widget_html");
            if (!nextStreamsHtmlDataRaw.isEmpty()) {
                nextStreamsUrl = getNextStreamsUrlFrom(Jsoup.parse(nextStreamsHtmlDataRaw, nextStreamsUrl));
            } else {
                nextStreamsUrl = "";
            }
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json data for next streams", e);
        }
    }

    private String getNextStreamsUrlFrom(Document d) throws ParsingException {
        try {
            Element button = d.select("button[class*=\"yt-uix-load-more\"]").first();
            if (button != null) {
                return button.attr("abs:data-uix-load-more-href");
            } else {
                // Sometimes channels are simply so small, they don't have a more streams/videos
                return "";
            }
        } catch (Exception e) {
            throw new ParsingException("could not get next streams' url", e);
        }
    }

    private void collectStreamsFrom(StreamInfoItemCollector collector, Element element) throws ParsingException {
        collector.getItemList().clear();

        final String uploaderName = getName();
        for (final Element li : element.children()) {
            if (li.select("div[class=\"feed-item-dismissable\"]").first() != null) {
                collector.commit(new YoutubeStreamInfoItemExtractor(li) {
                    @Override
                    public String getUrl() throws ParsingException {
                        try {
                            Element el = li.select("div[class=\"feed-item-dismissable\"]").first();
                            Element dl = el.select("h3").first().select("a").first();
                            return dl.attr("abs:href");
                        } catch (Exception e) {
                            throw new ParsingException("Could not get web page url for the video", e);
                        }
                    }

                    @Override
                    public String getName() throws ParsingException {
                        try {
                            Element el = li.select("div[class=\"feed-item-dismissable\"]").first();
                            Element dl = el.select("h3").first().select("a").first();
                            return dl.text();
                        } catch (Exception e) {
                            throw new ParsingException("Could not get title", e);
                        }
                    }

                    @Override
                    public String getUploaderName() throws ParsingException {
                        return uploaderName;
                    }

                    @Override
                    public String getThumbnailUrl() throws ParsingException {
                        try {
                            String url;
                            Element te = li.select("span[class=\"yt-thumb-clip\"]").first()
                                    .select("img").first();
                            url = te.attr("abs:src");
                            // Sometimes youtube sends links to gif files which somehow seem to not exist
                            // anymore. Items with such gif also offer a secondary image source. So we are going
                            // to use that if we've caught such an item.
                            if (url.contains(".gif")) {
                                url = te.attr("abs:data-thumb");
                            }
                            return url;
                        } catch (Exception e) {
                            throw new ParsingException("Could not get thumbnail url", e);
                        }
                    }
                });
            }
        }
    }
}
