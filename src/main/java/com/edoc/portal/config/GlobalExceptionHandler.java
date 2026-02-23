package com.edoc.portal.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size:1MB}")
    private String maxFileSize;

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String handleMultipartException(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "error",
                "Uploaded file is too large. Maximum allowed size is " + maxFileSize + "."
        );

        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        if (uri.startsWith("/customer/apply")) {
            return "redirect:/customer/apply";
        }
        if (uri.startsWith("/csc/application")) {
            return "redirect:/csc/manage-applications";
        }
        return "redirect:/login";
    }
}
