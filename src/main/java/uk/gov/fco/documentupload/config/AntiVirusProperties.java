package uk.gov.fco.documentupload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "antivirus")
@Data
public class AntiVirusProperties {

    private String host;

    private int port;

    private int timeout;

    private boolean enabled;
}
