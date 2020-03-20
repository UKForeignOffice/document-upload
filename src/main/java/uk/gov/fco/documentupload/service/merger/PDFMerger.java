package uk.gov.fco.documentupload.service.merger;

import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public FileUpload merge(List<FileUpload> uploads) {
        return null;
    }
}
