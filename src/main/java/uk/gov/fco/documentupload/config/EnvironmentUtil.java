package uk.gov.fco.documentupload.config;

import org.springframework.core.env.Environment;

public class EnvironmentUtil {
    public static boolean isDevelopment(Environment environment) {
        if (environment != null) {
            String[] activeProfiles = environment.getActiveProfiles();
            for (String profile : activeProfiles) {
                if (profile.equalsIgnoreCase("development")) {
                    return true;
                }
            }
        }
        return false;
    }
}
