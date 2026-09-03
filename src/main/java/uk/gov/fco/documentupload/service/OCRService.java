package uk.gov.fco.documentupload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsImageProperties;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.Image;
import uk.gov.fco.documentupload.OcrProperties;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OCRService {

    private final OcrProperties properties;
    private final RekognitionClient rekognition;

    public boolean passesQualityCheck(FileUpload upload) throws IOException {
        if (properties.enabled() && !Objects.equals(upload.getContentType(), "application/pdf")) {
            log.info("Starting image quality check");
            try {
                SdkBytes bytes = SdkBytes.fromInputStream(upload.getInputStream());
                Image image = Image.builder().bytes(bytes).build();
                DetectLabelsRequest request = DetectLabelsRequest.builder().image(image).featuresWithStrings("IMAGE_PROPERTIES").build();
                DetectLabelsImageProperties results = rekognition.detectLabels(request).imageProperties();
                Float sharpness = results.quality().sharpness();
                log.info(String.format("Image sharpness: %s", sharpness.toString()));
                boolean result = sharpness >= properties.sharpness().threshold();
                log.info(String.format("Passes quality check: %s", result));
                return result;
            } catch (Exception err) {
                log.error("Image quality check failed, bypassing");
                return true;
            }
        }
        return true;
    }
}
