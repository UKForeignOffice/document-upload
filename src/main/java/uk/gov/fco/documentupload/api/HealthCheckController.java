package uk.gov.fco.documentupload.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/health")
@Slf4j
public class HealthCheckController {

    public HealthCheckController() {
    }

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
    public void retrieve(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        log.trace("Flushing response buffer for health check");
        response.flushBuffer();
    }
}
