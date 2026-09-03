package uk.gov.fco.documentupload.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated use {@code /actuator/health} instead
 */
@RestController
@RequestMapping("/health")
@Slf4j
@Deprecated
public class HealthController {

    @GetMapping("")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is running")
    })
    @Operation(
            summary = "Health check",
            description = "Used by a load balancer to determine if the service is running"
    )
    public ResponseEntity<Void> status() {
        return ResponseEntity.ok().build();
    }
}
