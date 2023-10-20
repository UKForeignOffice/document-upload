package uk.gov.fco.documentupload.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class PayloadTooLargeExceptionResolver extends AbstractHandlerExceptionResolver {
    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex instanceof MaxUploadSizeExceededException) {
            log.debug("Handling file size too large exception");
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            try {
                response.getWriter().write("fileSizeError");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new ModelAndView();
        }
        return null;
    }
}
