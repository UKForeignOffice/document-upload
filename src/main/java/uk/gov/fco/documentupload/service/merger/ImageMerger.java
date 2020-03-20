package uk.gov.fco.documentupload.service.merger;

import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

@Service
public class ImageMerger implements Merger {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<String>() {{
        add("image/png");
        add("image/jpeg");
        add("image/jpg");
        add("image/gif");
    }};

    private static final int PADDING = 20;

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

        List<BufferedImage> images = new ArrayList<>();
        int width = 0;
        int height = -PADDING;

        for (FileUpload upload : uploads) {
            BufferedImage image = ImageIO.read(upload.getInputStream());
            images.add(image);

            width = Math.max(width, image.getWidth());
            height += image.getHeight() + PADDING;
        }

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = combined.getGraphics();
        int currentY = 0;

        for (BufferedImage image : images) {
            graphics.drawImage(image, (width - image.getWidth()) / 2, currentY, null);
            currentY += image.getHeight() + PADDING;
        }

        File file = Files.createTempFile(UUID.randomUUID().toString(), ".png").toFile();
        ImageIO.write(combined, "PNG", file);

        graphics.dispose();

        return new FileUpload("image/png", file);
    }
}
