package com.edoc.portal.controller;

import com.edoc.portal.entity.User;
import com.edoc.portal.enums.Role;
import com.edoc.portal.service.PortalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final PortalService portalService;
    private final AuthenticationManager authenticationManager;

    public AuthController(PortalService portalService, AuthenticationManager authenticationManager) {
        this.portalService = portalService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String customerLogin() {
        return "customer-login";
    }

    @GetMapping("/csc/login")
    public String cscLogin() {
        return "csc-login";
    }

    @GetMapping("/admin/login")
    public String adminLogin(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid admin credentials.");
        }
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String adminLoginSubmit(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (!isAdmin) {
                throw new BadCredentialsException("Invalid admin user.");
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
            return "redirect:/admin/dashboard";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Invalid admin credentials.");
            return "redirect:/admin/login";
        }
    }

    @GetMapping("/register")
    public String customerRegister(Model model) {
        model.addAttribute("cscUsers", portalService.getApprovedCscUsers());
        return "customer-register";
    }

    @PostMapping("/register")
    public String registerCustomer(@RequestParam String fullName,
                                   @RequestParam String email,
                                   @RequestParam String mobile,
                                   @RequestParam String assignedCscEmail,
                                   @RequestParam String password,
                                   RedirectAttributes redirectAttributes) {
        try {
            portalService.registerCustomer(fullName, email, mobile, password, assignedCscEmail);
            redirectAttributes.addFlashAttribute("success", "Registration submitted. Wait for CSC approval.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/csc/register")
    public String cscRegister() {
        return "csc-register";
    }

    @PostMapping("/csc/register")
    public String registerCsc(@RequestParam String cscPortalName,
                              @RequestParam String ownerName,
                              @RequestParam String cscId,
                              @RequestParam String mobileNumber,
                              @RequestParam String cscCenterAddress,
                              @RequestParam String usernameEmail,
                              @RequestParam String password,
                              RedirectAttributes redirectAttributes) {
        try {
            portalService.registerCsc(ownerName, usernameEmail, password, cscPortalName, cscId, mobileNumber, cscCenterAddress, "Pending");
            redirectAttributes.addFlashAttribute("success", "CSC registration submitted. Wait for admin approval.");
            return "redirect:/csc/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/csc/register";
        }
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("activeCscRequests", portalService.getCscUsers().stream()
                .filter(u -> !"DELETED".equalsIgnoreCase(u.getStatus()))
                .toList());
        model.addAttribute("deletedCscRequests", portalService.getCscUsers().stream()
                .filter(u -> "DELETED".equalsIgnoreCase(u.getStatus()))
                .toList());
        return "admin-dashboard";
    }

    @PostMapping("/admin/csc-users/manual")
    public String createCsc(@RequestParam String cscPortalName,
                            @RequestParam String ownerName,
                            @RequestParam String cscId,
                            @RequestParam String mobileNumber,
                            @RequestParam String cscCenterAddress,
                            @RequestParam String usernameEmail,
                            @RequestParam String password,
                            RedirectAttributes redirectAttributes) {
        try {
            portalService.registerCsc(ownerName, usernameEmail, password, cscPortalName, cscId, mobileNumber, cscCenterAddress, "ACTIVE");
            redirectAttributes.addFlashAttribute("success", "CSC user created successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/csc-users/edit/{id}")
    public String editCscForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = portalService.findUserById(id).orElse(null);
        if (user == null || user.getRole() != Role.CSC) {
            redirectAttributes.addFlashAttribute("error", "CSC user not found.");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("cscRequest", user);
        return "admin-csc-edit";
    }

    @PostMapping("/admin/csc-users/edit/{id}")
    public String editCsc(@PathVariable Long id,
                          @RequestParam String cscPortalName,
                          @RequestParam String ownerName,
                          @RequestParam String cscId,
                          @RequestParam String mobileNumber,
                          @RequestParam String cscCenterAddress,
                          @RequestParam String usernameEmail,
                          @RequestParam(required = false) String password,
                          RedirectAttributes redirectAttributes) {
        User user = portalService.findUserById(id).orElse(null);
        if (user == null || user.getRole() != Role.CSC) {
            redirectAttributes.addFlashAttribute("error", "CSC user not found.");
            return "redirect:/admin/dashboard";
        }

        user.setCscPortalName(cscPortalName);
        user.setOwnerName(ownerName);
        user.setCscId(cscId);
        user.setMobileNumber(mobileNumber);
        user.setCscCenterAddress(cscCenterAddress);
        user.setUsernameEmail(usernameEmail);
        if (password != null && !password.isBlank()) {
            user.setPassword(portalService.encodePassword(password));
        }
        portalService.saveUser(user);
        redirectAttributes.addFlashAttribute("success", "CSC user updated.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/csc-users/request/approve/{id}")
    public String approveCsc(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            portalService.updateUserStatus(id, "ACTIVE");
            redirectAttributes.addFlashAttribute("success", "CSC user activated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/csc-users/delete/{id}")
    public String deleteCsc(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            portalService.updateUserStatus(id, "DELETED");
            redirectAttributes.addFlashAttribute("success", "CSC user deactivated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/logout")
    public String adminLogout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/";
    }
}
