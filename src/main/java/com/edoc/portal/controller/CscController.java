package com.edoc.portal.controller;

import com.edoc.portal.entity.Application;
import com.edoc.portal.entity.User;
import com.edoc.portal.enums.ApplicationStatus;
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
import java.util.Locale;

@Controller
@RequestMapping("/csc")
public class CscController {

    private final PortalService portalService;

    public CscController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "csc-dashboard";
    }

    @GetMapping("/customers")
    public String customers(Authentication auth, Model model) {
        String cscEmail = currentEmail(auth);
        var deactivatedCustomers = portalService.getPendingCustomersForCsc(cscEmail);
        model.addAttribute("deactivatedCustomers", deactivatedCustomers);
        model.addAttribute("activeCustomers", portalService.getActiveCustomersForCsc(cscEmail));
        return "csc-manage-customers";
    }

    @GetMapping("/manage-customers")
    public String manageCustomers(Authentication auth, Model model) {
        return customers(auth, model);
    }

    @PostMapping("/customers/manual")
    public String createCustomerManually(@RequestParam String fullName,
                                         @RequestParam String email,
                                         @RequestParam String mobile,
                                         @RequestParam String password,
                                         Authentication auth,
                                         RedirectAttributes redirectAttributes) {
        try {
            String cscEmail = currentEmail(auth);
            portalService.registerCustomer(fullName, email, mobile, password, cscEmail);
            User user = portalService.findUserByEmail(email).orElse(null);
            if (user != null) {
                portalService.updateUserStatus(user.getId(), "ACTIVE");
            }
            redirectAttributes.addFlashAttribute("success", "Customer created and activated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/csc/customers";
    }

    @GetMapping("/approve-customer/{id}")
    public String approveCustomer(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!portalService.isCustomerAssignedToCsc(id, currentEmail(auth))) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to approve this customer.");
            return "redirect:/csc/customers";
        }
        portalService.updateUserStatus(id, "ACTIVE");
        redirectAttributes.addFlashAttribute("success", "Customer approved.");
        return "redirect:/csc/customers";
    }

    @GetMapping("/deactivate-customer/{id}")
    public String deactivateCustomer(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!portalService.isCustomerAssignedToCsc(id, currentEmail(auth))) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to deactivate this customer.");
            return "redirect:/csc/customers";
        }
        portalService.updateUserStatus(id, "PENDING");
        redirectAttributes.addFlashAttribute("success", "Customer deactivated.");
        return "redirect:/csc/customers";
    }

    @GetMapping("/remove-customer/{id}")
    public String removeCustomer(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!portalService.isCustomerAssignedToCsc(id, currentEmail(auth))) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to delete this customer.");
            return "redirect:/csc/customers";
        }
        portalService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "Customer deleted.");
        return "redirect:/csc/customers";
    }

    @GetMapping("/applications")
    public String applications(Authentication auth, Model model) {
        String cscEmail = currentEmail(auth);
        List<Application> all = portalService.getAllApplicationsForCsc(cscEmail);
        model.addAttribute("applications", all);
        model.addAttribute("pendingList", all.stream().filter(a -> hasStatus(a, "PENDING")).toList());
        model.addAttribute("appliedList", all.stream().filter(a -> hasStatus(a, "APPLIED", "IN_PROCESS")).toList());
        model.addAttribute("successList", all.stream().filter(a -> hasStatus(a, "SUCCESS", "REJECTED", "APPROVED", "ISSUED")).toList());
        return "csc-manage-applications";
    }

    @GetMapping("/manage-applications")
    public String manageApplications(Authentication auth, Model model) {
        return applications(auth, model);
    }

    @GetMapping("/application/{id}")
    public String applicationDetail(@PathVariable Long id,
                                    @RequestParam(required = false) Long previewDocId,
                                    @RequestParam(required = false) String status,
                                    Model model,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        Application app = portalService.findApplicationById(id).orElse(null);
        if (app == null) {
            redirectAttributes.addFlashAttribute("error", "Application not found.");
            return "redirect:/csc/applications";
        }
        if (!portalService.isApplicationAssignedToCsc(id, currentEmail(auth))) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to view this application.");
            return "redirect:/csc/applications";
        }

        String selectedStatus = status == null || status.isBlank() ? app.getStatus() : status;
        model.addAttribute("app", app);
        model.addAttribute("application", app);
        model.addAttribute("customer", app.getCustomer());
        model.addAttribute("statuses", List.of(
                ApplicationStatus.PENDING,
                ApplicationStatus.APPLIED,
                ApplicationStatus.SUCCESS,
                ApplicationStatus.REJECTED
        ));
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("previewDocId", previewDocId);
        return "csc-application-detail";
    }

    @PostMapping("/application/update-status")
    public String updateStatus(@RequestParam Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) MultipartFile issuedDocument,
                               @RequestParam(required = false) String message,
                               @RequestParam(required = false) String rejectionMessage,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        if (!portalService.isApplicationAssignedToCsc(id, currentEmail(auth))) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to update this application.");
            return "redirect:/csc/applications";
        }
        try {
            String finalMessage = (rejectionMessage != null && !rejectionMessage.isBlank()) ? rejectionMessage : message;
            portalService.updateApplicationStatus(id, status, finalMessage, issuedDocument);
            redirectAttributes.addFlashAttribute("success", "Application status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/csc/application/" + id;
    }

    @GetMapping("/application/document/{docId}/download")
    public ResponseEntity<?> downloadApplicationDocument(@PathVariable Long docId, Authentication auth) {
        Long appId = docId / 1000L;
        if (!portalService.isApplicationAssignedToCsc(appId, currentEmail(auth))) {
            return ResponseEntity.status(403).build();
        }
        try {
            PortalService.DocumentFile file = portalService.getUploadedDocumentByPseudoId(docId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                    .contentType(MediaType.parseMediaType(file.contentType()))
                    .body(file.bytes());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to download document.");
        }
    }

    @GetMapping("/application/document/{docId}/view")
    public ResponseEntity<?> viewApplicationDocument(@PathVariable Long docId, Authentication auth) {
        Long appId = docId / 1000L;
        if (!portalService.isApplicationAssignedToCsc(appId, currentEmail(auth))) {
            return ResponseEntity.status(403).build();
        }
        try {
            PortalService.DocumentFile file = portalService.getUploadedDocumentByPseudoId(docId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.contentType()))
                    .body(file.bytes());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to view document.");
        }
    }

    @GetMapping("/application/{id}/download-issued")
    public ResponseEntity<?> downloadIssued(@PathVariable Long id, Authentication auth) {
        if (!portalService.isApplicationAssignedToCsc(id, currentEmail(auth))) {
            return ResponseEntity.status(403).build();
        }
        try {
            PortalService.DocumentFile file = portalService.getIssuedDocumentFile(id, "", true);
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

    private String currentEmail(Authentication auth) {
        return auth == null || auth.getName() == null ? "" : auth.getName().trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasStatus(Application application, String... statuses) {
        if (application == null || application.getStatus() == null) {
            return false;
        }
        String actual = application.getStatus().trim().toUpperCase(Locale.ROOT);
        for (String status : statuses) {
            if (status != null && actual.equals(status.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
