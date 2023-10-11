package uk.gov.fco.documentupload.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.model.*;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import javax.validation.constraints.NotNull;
import java.io.IOException;

import software.amazon.awssdk.services.rekognition.RekognitionClient;

@Service
@Slf4j
public class OCRService {
    private boolean enabled;

    private int sharpnessThreshold;

    private RekognitionClient rekognition;

    public OCRService(@Value("${ocr.enabled}") @NotNull boolean enabled, @Value("${ocr.sharpness.threshold}") int sharpnessThreshold){
        this.enabled = enabled;
        this.sharpnessThreshold = sharpnessThreshold;
        rekognition = RekognitionClient.builder().build();
    }

    public boolean passesQualityCheck(FileUpload upload) throws IOException {
        if(enabled){
            log.trace("Starting image quality check");
            SdkBytes bytes = SdkBytes.fromInputStream(upload.getInputStream());
            Image image = Image.builder().bytes(bytes).build();
            DetectLabelsRequest request = DetectLabelsRequest.builder().image(image).featuresWithStrings("IMAGE_PROPERTIES").build();
            DetectLabelsImageProperties results = rekognition.detectLabels(request).imageProperties();
            return results.quality().sharpness() >= sharpnessThreshold;
        }
        return true;
    }
}
