package uk.gov.fco.documentupload.service.fileCheck;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class FileCheckService {

    private Tika tika;
    private static final Set<String> VALID_CONTENT_TYPES = new HashSet<String>() {{
        add("application/pdf");
        add("image/png");
        add("image/jpeg");
        add("image/jpg");
        add("image/gif");
    }};

    public FileCheckService() {
        this.tika = new Tika();
    }

    public Boolean isValidFileType(InputStream file, String fileName) {
        try {
            String mimeType = this.tika.detect(file, fileName);
            return VALID_CONTENT_TYPES.contains(mimeType);
        } catch (IOException e) {
            log.error("Could not determine file MIME type", e);
            return false;
        }
    }

}
