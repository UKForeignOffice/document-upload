package uk.gov.fco.documentupload.service.antivirus;

import fi.solita.clamav.ClamAVClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class AntiVirusService {

    private ClamAVClient clamAVClient;

    @Autowired
    public AntiVirusService(@NonNull ClamAVClient clamAVClient) {
        this.clamAVClient = clamAVClient;
    }

    public boolean isClean(FileUpload upload) throws IOException {
//        log.trace("Scanning file for viruses");
//        byte[] reply = clamAVClient.scan(upload.getInputStream());
//        if (!ClamAVClient.isCleanReply(reply)) {
//            String detail = new String(reply, StandardCharsets.US_ASCII);
//            log.warn("File contains virus, detail = {}", detail);
//            return false;
//        }
        return true;
    }
}
