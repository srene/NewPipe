package io.fluentic.ubicdn.extractor.kiosk;

/*
 * Created by Christian Schabesberger on 12.08.17.
 *
 * Copyright (C) Christian Schabesberger 2017 <chris.schabesberger@mailbox.org>
 * KioskExtractor.java is part of NewPipe.
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

import io.fluentic.ubicdn.extractor.ListExtractor;
import io.fluentic.ubicdn.extractor.StreamingService;
import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;

import java.io.IOException;

public abstract class KioskExtractor extends ListExtractor {
    private String contentCountry = null;
    private String id = null;

    public KioskExtractor(StreamingService streamingService,
                          String url,
                          String nextStreamsUrl,
                          String kioskId)
        throws IOException, ExtractionException {
        super(streamingService, url, nextStreamsUrl);
        this.id = kioskId;
    }

    /**
     * For certain Websites the content of a kiosk will be different depending
     * on the country you want to poen the website in. Therefore you should
     * set the contentCountry.
     * @param contentCountry Set the country decoded as Country Code: http://www.1728.org/countries.htm
     */
    public void setContentCountry(String contentCountry) {
        this.contentCountry = contentCountry;
    }


    @Override
    public String getId() throws ParsingException {
        return id;
    }

    /**
     * Id should be the name of the kiosk, tho Id is used for identifing it in the programm,
     * so id should be kept in english.
     * In order to get the name of the kiosk in the desired language we have to
     * crawl if from the website.
     * @return the tranlsated version of id
     * @throws ParsingException
     */
    @Override
    public abstract String getName() throws ParsingException;


    public String getContentCountry() {
        return contentCountry;
    }
}
