package uk.gov.fco.documentupload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr")
public record OcrProperties(boolean enabled, Sharpness sharpness) {

    public record Sharpness(int threshold) {}
}
