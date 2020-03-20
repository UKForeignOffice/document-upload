package uk.gov.fco.documentupload.service.merger;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PDFMerger implements Merger {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<String>() {{
        add("application/pdf");
    }};

    @Override
    public boolean supports(List<FileUpload> uploads) {
        for (FileUpload upload : uploads) {
            if (!SUPPORTED_CONTENT_TYPES.contains(upload.getContentType())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public FileUpload merge(List<FileUpload> uploads) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        for (FileUpload upload : uploads) {
            merger.addSource(upload.getInputStream());
        }

        File file = Files.createTempFile(UUID.randomUUID().toString(), ".pdf").toFile();

        merger.setDestinationFileName(file.getAbsolutePath());
        merger.mergeDocuments(MemoryUsageSetting.setupMixed(20 * 1024 * 1024));

        return new FileUpload("application/pdf", file);
    }
}
