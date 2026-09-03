package uk.gov.fco.documentupload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "antivirus")
public record AntiVirusProperties(String host, int port, int timeout, boolean enabled) {
}
