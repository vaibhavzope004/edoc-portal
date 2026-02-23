package com.edoc.portal.controller;

import com.edoc.portal.entity.Application;
import com.edoc.portal.service.PortalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final PortalService portalService;

    public CustomerController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        List<Application> applications = portalService.getCustomerApplications(auth.getName());
        model.addAttribute("requests", applications);
        return "customer-dashboard";
    }

    @GetMapping("/apply")
    public String applyPage(@RequestParam(required = false) String serviceType, Model model) {
        model.addAttribute("services", portalService.getServiceTypes());
        model.addAttribute("selectedService", serviceType);
        model.addAttribute("requiredDocs", portalService.getRequiredDocuments(serviceType));
        return "apply-service";
    }

    @PostMapping("/apply")
    public String apply(@RequestParam String serviceType,
                        @RequestParam String name,
                        @RequestParam String mobile,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false, defaultValue = "false") boolean paymentDone,
                        @RequestParam(required = false) MultipartFile[] documents,
                        Authentication auth,
                        RedirectAttributes redirectAttributes) {
        try {
            portalService.applyForService(auth.getName(), serviceType, name, mobile, description, paymentDone, documents);
            redirectAttributes.addFlashAttribute("success", "Application submitted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/dashboard";
    }

    @GetMapping("/applications")
    public String applications(Authentication auth, Model model) {
        model.addAttribute("requests", portalService.getCustomerApplications(auth.getName()));
        return "customer-dashboard";
    }

    @GetMapping("/application/{id}/issued-document/download")
    public ResponseEntity<?> download(@PathVariable Long id, Authentication auth) {
        try {
            PortalService.DocumentFile file = portalService.getIssuedDocumentFile(id, auth.getName(), false);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                    .contentType(MediaType.parseMediaType(file.contentType()))
                    .body(file.bytes());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to download issued document.");
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/";
    }
}
