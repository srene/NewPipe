package io.fluentic.ubicdn.extractor;

import io.fluentic.ubicdn.extractor.exceptions.ParsingException;

public interface InfoItemExtractor {
    String getName() throws ParsingException;
    String getUrl() throws ParsingException;
    String getThumbnailUrl() throws ParsingException;
}
