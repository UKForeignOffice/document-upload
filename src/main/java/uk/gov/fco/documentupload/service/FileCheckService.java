package uk.gov.fco.documentupload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.AppProperties;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileCheckService {

    private final Tika tika;
    private final AppProperties properties;

    public boolean isValidFileType(final InputStream file, final String fileName) {
        try {
            return properties.mimeTypes().contains(tika.detect(file, fileName));
        } catch (IOException e) {
            log.error("Could not determine file MIME type", e);
            return false;
        }
    }
}
