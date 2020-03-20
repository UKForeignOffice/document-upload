package uk.gov.fco.documentupload.service.merger;

import lombok.NonNull;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class PDFAndImageMerger implements Merger {

    private static final Set<String> PDF_CONTENT_TYPES = new HashSet<String>() {{
        add("application/pdf");
    }};

    private static final Set<String> IMAGE_CONTENT_TYPES = new HashSet<String>() {{
        add("image/png");
        add("image/jpeg");
        add("image/jpg");
        add("image/gif");
    }};

    private ImageMerger imageMerger;

    @Autowired
    public PDFAndImageMerger(@NonNull ImageMerger imageMerger) {
        this.imageMerger = imageMerger;
    }

    @Override
    public boolean supports(List<FileUpload> uploads) {
        boolean hasPDF = false;
        boolean hasImage = false;
        for (FileUpload upload : uploads) {
            if (PDF_CONTENT_TYPES.contains(upload.getContentType())) {
                hasPDF = true;
            } else if (IMAGE_CONTENT_TYPES.contains(upload.getContentType())) {
                hasImage = true;
            }
        }
        return hasImage && hasPDF;
    }

    @Override
    public FileUpload merge(List<FileUpload> uploads) throws IOException {
        List<FileUpload> converted = new ArrayList<>();
        for (FileUpload upload : uploads) {
            if (PDF_CONTENT_TYPES.contains(upload.getContentType())) {
                converted.addAll(convertToImages(upload));
            } else {
                converted.add(upload);
            }
        }
        return imageMerger.merge(converted);
    }

    private List<FileUpload> convertToImages(FileUpload upload) throws IOException {
        List<FileUpload> converted = new ArrayList<>();
        try (PDDocument document = PDDocument.load(upload.getInputStream())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                File file = Files.createTempFile(UUID.randomUUID().toString(), ".png").toFile();
                ImageIO.write(renderer.renderImage(page), "PNG", file);
                converted.add(new FileUpload("image/png", file));
            }
        }
        return converted;
    }
}
