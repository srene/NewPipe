package io.fluentic.ubicdn.extractor.playlist;

import io.fluentic.ubicdn.extractor.InfoItemExtractor;
import io.fluentic.ubicdn.extractor.exceptions.ParsingException;

public interface PlaylistInfoItemExtractor extends InfoItemExtractor {
    String getUploaderName() throws ParsingException;
    long getStreamCount() throws ParsingException;
}
