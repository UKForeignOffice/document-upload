package uk.gov.fco.documentupload.service;

import fi.solita.clamav.ClamAVClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.AntiVirusProperties;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class AntiVirusService {

    private final ClamAVClient client;
    private final AntiVirusProperties properties;

    public boolean isClean(FileUpload upload) throws IOException {
        if (properties.enabled()) {
            log.trace("Scanning file for viruses");
            byte[] reply = client.scan(upload.getInputStream());
            if (!ClamAVClient.isCleanReply(reply)) {
                String detail = new String(reply, StandardCharsets.US_ASCII);
                log.warn("File contains virus, detail = {}", detail);
                return false;
            }
        }
        return true;
    }
}
