package com.edoc.portal.service;

import com.edoc.portal.entity.Application;
import com.edoc.portal.entity.ApplicationDocument;
import com.edoc.portal.entity.AdminProfile;
import com.edoc.portal.entity.User;
import com.edoc.portal.enums.Role;
import com.edoc.portal.repository.ApplicationRepository;
import com.edoc.portal.repository.CscUserProfileRepository;
import com.edoc.portal.repository.CustomerProfileRepository;
import com.edoc.portal.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PortalService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CscUserProfileRepository cscUserProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public PortalService(UserRepository userRepository,
                         ApplicationRepository applicationRepository,
                         CscUserProfileRepository cscUserProfileRepository,
                         CustomerProfileRepository customerProfileRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.cscUserProfileRepository = cscUserProfileRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Central service-document configuration used by apply form and upload labeling.
    private static final List<ServiceDefinition> SERVICE_DEFINITIONS = List.of(
            new ServiceDefinition("Nationality Certificate", List.of(
                    "Applicant Leaving Certificate / TC / Bonafide",
                    "Aadhaar Card",
                    "Applicant Photo",
                    "Ration Card",
                    "Father Leaving Certificate / TC",
                    "Self Declaration - Mandatory"
            )),
            new ServiceDefinition("Domicile Certificate", List.of(
                    "Applicant Leaving Certificate / TC / Bonafide",
                    "Aadhaar Card",
                    "Applicant Photo",
                    "Ration Card",
                    "Father Leaving Certificate / TC",
                    "Self Declaration - Mandatory"
            )),
            new ServiceDefinition("Caste Certificate", List.of(
                    "Applicant Leaving Certificate / TC / Bonafide",
                    "Aadhaar Card",
                    "Applicant Photo",
                    "Ration Card",
                    "Father Leaving Certificate / TC",
                    "Grandfather Leaving Certificate - Mandatory",
                    "Self Declaration - Mandatory"
            )),
            new ServiceDefinition("Non-Creamy Layer Certificate", List.of(
                    "Applicant Leaving Certificate / TC / Bonafide",
                    "Aadhaar Card",
                    "Applicant Photo",
                    "Ration Card",
                    "Father Leaving Certificate / TC",
                    "Income Proof - Last 3 Years",
                    "Income Certificate from Tehsildar",
                    "Self Declaration - Mandatory"
            )),
            new ServiceDefinition("Income Certificate", List.of(
                    "Income Proof from Talathi",
                    "Aadhaar Card",
                    "Ration Card",
                    "Self Declaration - Mandatory"
            )),
            new ServiceDefinition("PAN Card", List.of(
                    "Aadhaar Card",
                    "Applicant Photo"
            ))
    );

    @PostConstruct
    public void init() {
        User admin = userRepository.findByEmail("vaibhav")
                .orElseGet(() -> userRepository.findByRole(Role.ADMIN).stream().findFirst().orElse(new User()));
        admin.setName("Vaibhav");
        admin.setEmail("vaibhav");
        admin.setPassword(passwordEncoder.encode("Password1234@"));
        admin.setRole(Role.ADMIN);
        admin.setStatus("ACTIVE");
        if (admin.getAdminProfile() == null) {
            AdminProfile adminProfile = new AdminProfile();
            adminProfile.setDisplayName("Vaibhav");
            admin.setAdminProfile(adminProfile);
        } else {
            admin.getAdminProfile().setDisplayName("Vaibhav");
        }
        userRepository.save(admin);
    }

    public User register(String name, String email, String password, Role role) {
        if (role == Role.CUSTOMER) {
            return registerCustomer(name, email, null, password, null);
        }
        if (role == Role.CSC) {
            return registerCsc(name, email, password, null, null, null, null, "ACTIVE");
        }
        throw new IllegalArgumentException("Unsupported role.");
    }

    public User registerCustomer(String fullName, String email, String mobile, String password, String assignedCscEmail) {
        String normalizedEmail = normalize(email);
        String trimmedMobile = trim(mobile);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (trimmedMobile != null && !trimmedMobile.isBlank()
                && customerProfileRepository.existsByMobileNumber(trimmedMobile)) {
            throw new IllegalArgumentException("Mobile number already exists.");
        }
        String normalizedAssignedCsc = normalize(assignedCscEmail);
        if (normalizedAssignedCsc.isBlank()) {
            throw new IllegalArgumentException("Please select a CSC user.");
        }
        User assignedCsc = userRepository.findByEmail(normalizedAssignedCsc)
                .orElseThrow(() -> new IllegalArgumentException("Assigned CSC user not found."));
        if (assignedCsc.getRole() != Role.CSC || !"ACTIVE".equalsIgnoreCase(assignedCsc.getStatus())) {
            throw new IllegalArgumentException("Assigned CSC user is not active.");
        }

        User user = new User();
        user.setRole(Role.CUSTOMER);
        user.setFullName(trim(fullName));
        user.setEmail(normalizedEmail);
        user.setMobileNumber(trimmedMobile);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus("PENDING");
        user.setAssignedCscEmail(normalizedAssignedCsc);
        return userRepository.save(user);
    }

    public User registerCsc(String ownerName,
                            String usernameEmail,
                            String password,
                            String cscPortalName,
                            String cscId,
                            String mobileNumber,
                            String cscCenterAddress,
                            String status) {
        String normalizedEmail = normalize(usernameEmail);
        String trimmedCscId = trim(cscId);
        String trimmedMobile = trim(mobileNumber);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (trimmedCscId != null && !trimmedCscId.isBlank()
                && cscUserProfileRepository.existsByCscId(trimmedCscId)) {
            throw new IllegalArgumentException("CSC ID already exists.");
        }
        if (trimmedMobile != null && !trimmedMobile.isBlank()
                && cscUserProfileRepository.existsByMobileNumber(trimmedMobile)) {
            throw new IllegalArgumentException("Mobile number already exists.");
        }
        User user = new User();
        user.setRole(Role.CSC);
        user.setOwnerName(trim(ownerName));
        user.setEmail(normalizedEmail);
        user.setMobileNumber(trimmedMobile);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(status == null || status.isBlank() ? "ACTIVE" : status.toUpperCase(Locale.ROOT));
        user.setCscPortalName(trim(cscPortalName));
        user.setCscId(trimmedCscId);
        user.setCscCenterAddress(trim(cscCenterAddress));
        return userRepository.save(user);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(normalize(email));
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public List<User> getCscUsers() {
        return userRepository.findByRole(Role.CSC);
    }

    public List<User> getApprovedCscUsers() {
        return getCscUsers().stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).toList();
    }

    public List<User> getPendingCustomersForCsc(String cscEmail) {
        String normalizedCscEmail = normalize(cscEmail);
        return userRepository.findByRole(Role.CUSTOMER).stream()
                .filter(u -> "PENDING".equalsIgnoreCase(u.getStatus()))
                .filter(u -> matchesAssignedCsc(u.getAssignedCscEmail(), normalizedCscEmail))
                .toList();
    }


    public List<User> getActiveCustomersForCsc(String cscEmail) {
        String normalizedCscEmail = normalize(cscEmail);
        return userRepository.findByRole(Role.CUSTOMER).stream()
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .filter(u -> matchesAssignedCsc(u.getAssignedCscEmail(), normalizedCscEmail))
                .toList();
    }

    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setStatus(status.toUpperCase(Locale.ROOT));
        userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public boolean isCustomerAssignedToCsc(Long customerId, String cscEmail) {
        User customer = userRepository.findById(customerId).orElse(null);
        if (customer == null || customer.getRole() != Role.CUSTOMER) {
            return false;
        }
        return matchesAssignedCsc(customer.getAssignedCscEmail(), normalize(cscEmail));
    }

    public Application applyForService(String customerEmail,
                                       String serviceType,
                                       String name,
                                       String mobile,
                                       String description,
                                       boolean paymentDone,
                                       MultipartFile[] documents) throws IOException {
        User customer = userRepository.findByEmail(normalize(customerEmail))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found."));

        Application app = new Application();
        app.setCustomer(customer);
        app.setApplicantName(trim(name));
        app.setApplicantMobile(trim(mobile));
        app.setServiceType(trim(serviceType));
        app.setDescription(trim(description));
        app.setStatus("PENDING");
        app.setAppliedDate(LocalDateTime.now());
        app.setDocumentPath("");
        app.setUploadedDocumentEntities(buildUploadedDocuments(app, serviceType, documents));
        return applicationRepository.save(app);
    }

    public List<Application> getCustomerApplications(String customerEmail) {
        User customer = userRepository.findByEmail(normalize(customerEmail)).orElse(null);
        if (customer == null) {
            return List.of();
        }
        return applicationRepository.findByCustomerOrderByAppliedDateDesc(customer);
    }

    public List<Application> getAllApplicationsForCsc(String cscEmail) {
        String normalizedCscEmail = normalize(cscEmail);
        return applicationRepository.findAllByOrderByAppliedDateDesc().stream()
                .filter(a -> a.getCustomer() != null)
                .filter(a -> matchesAssignedCsc(a.getCustomer().getAssignedCscEmail(), normalizedCscEmail))
                .toList();
    }

    public Optional<Application> findApplicationById(Long id) {
        return applicationRepository.findById(id);
    }

    public boolean isApplicationAssignedToCsc(Long applicationId, String cscEmail) {
        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app == null || app.getCustomer() == null) {
            return false;
        }
        return matchesAssignedCsc(app.getCustomer().getAssignedCscEmail(), normalize(cscEmail));
    }

    public void updateApplicationStatus(Long applicationId,
                                        String status,
                                        String message,
                                        MultipartFile issuedDocument) throws IOException {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        String normalizedStatus = status == null ? "" : status.toUpperCase(Locale.ROOT);
        app.setStatus(normalizedStatus);
        if (message != null && !message.isBlank()) {
            app.setMessage(message.trim());
        }
        // REJECTED is terminal: clear uploaded artifacts for this application.
        if ("REJECTED".equalsIgnoreCase(normalizedStatus)) {
            app.setDocumentPath(null);
            app.setIssuedDocumentPath(null);
            app.setIssuedDocumentName(null);
            app.setIssuedDocumentContentType(null);
            app.setIssuedDocumentData(null);
            app.getUploadedDocumentEntities().clear();
        } else if (issuedDocument != null && !issuedDocument.isEmpty()) {
            if (!isPdfFile(issuedDocument)) {
                throw new IllegalArgumentException("Issued document must be a PDF file.");
            }
            app.setIssuedDocumentName(sanitize(issuedDocument.getOriginalFilename() == null
                    ? "issued-document.pdf"
                    : issuedDocument.getOriginalFilename()));
            app.setIssuedDocumentContentType("application/pdf");
            app.setIssuedDocumentData(issuedDocument.getBytes());
            app.setIssuedDocumentPath(null);
        }
        applicationRepository.save(app);
    }

    public DocumentFile getIssuedDocumentFile(Long applicationId, String email, boolean allowAnyRole) throws IOException {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        if (!allowAnyRole && !normalize(email).equals(normalize(app.getCustomer().getEmail()))) {
            throw new IllegalArgumentException("Not authorized.");
        }
        if (app.getIssuedDocumentData() != null && app.getIssuedDocumentData().length > 0) {
            String fileName = app.getIssuedDocumentName() == null || app.getIssuedDocumentName().isBlank()
                    ? "issued-document.pdf"
                    : app.getIssuedDocumentName();
            String contentType = app.getIssuedDocumentContentType() == null || app.getIssuedDocumentContentType().isBlank()
                    ? "application/pdf"
                    : app.getIssuedDocumentContentType();
            return new DocumentFile(app.getIssuedDocumentData(), fileName, contentType);
        }
        throw new IllegalArgumentException("Issued document not available.");
    }

    public DocumentFile getUploadedDocumentByPseudoId(Long docId) throws IOException {
        long appId = docId / 1000L;
        int index = (int) (docId % 1000L) - 1;
        if (appId <= 0 || index < 0) {
            throw new IllegalArgumentException("Invalid document id.");
        }

        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        List<ApplicationDocument> dbDocs = app.getUploadedDocumentEntities();
        if (index >= dbDocs.size()) {
            throw new IllegalArgumentException("Document not found.");
        }
        ApplicationDocument dbDoc = dbDocs.get(index);
        String fileName = dbDoc.getFileName() == null || dbDoc.getFileName().isBlank() ? "document" : dbDoc.getFileName();
        String contentType = dbDoc.getContentType() == null || dbDoc.getContentType().isBlank()
                ? resolveContentType(fileName)
                : dbDoc.getContentType();
        return new DocumentFile(dbDoc.getData(), fileName, contentType);
    }

    private List<ApplicationDocument> buildUploadedDocuments(Application app,
                                                             String serviceType,
                                                             MultipartFile[] documents) throws IOException {
        if (documents == null || documents.length == 0) {
            return new ArrayList<>();
        }

        List<String> requiredDocs = getRequiredDocuments(serviceType);
        List<ApplicationDocument> items = new ArrayList<>();

        for (int i = 0; i < documents.length; i++) {
            MultipartFile file = documents[i];
            if (file == null || file.isEmpty()) {
                continue;
            }
            ApplicationDocument doc = new ApplicationDocument();
            doc.setApplication(app);
            doc.setSortOrder(i + 1);
            doc.setDocumentType(i < requiredDocs.size() ? requiredDocs.get(i) : "Document " + (i + 1));
            doc.setFileName(sanitize(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()));
            doc.setContentType(resolveUploadContentType(file));
            doc.setData(file.getBytes());
            items.add(doc);
        }
        return items;
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveUploadContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = originalName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isPdfFile(MultipartFile file) {
        String original = file.getOriginalFilename();
        String contentType = file.getContentType();
        boolean pdfByName = original != null && original.toLowerCase(Locale.ROOT).endsWith(".pdf");
        boolean pdfByType = contentType != null && contentType.equalsIgnoreCase("application/pdf");
        return pdfByName || pdfByType;
    }

    private String resolveContentType(String fileName) {
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerName.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    private boolean matchesAssignedCsc(String assignedCscEmail, String currentCscEmail) {
        String normalizedAssigned = normalize(assignedCscEmail);
        return normalizedAssigned.equals(currentCscEmail);
    }

    public List<String> getServiceTypes() {
        return SERVICE_DEFINITIONS.stream().map(ServiceDefinition::name).toList();
    }

    public List<String> getRequiredDocuments(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return List.of();
        }
        return SERVICE_DEFINITIONS.stream()
                .filter(s -> s.name().equalsIgnoreCase(serviceType.trim()))
                .findFirst()
                .map(ServiceDefinition::requiredDocuments)
                .orElse(List.of());
    }

    public record DocumentFile(byte[] bytes, String fileName, String contentType) {
    }

    private record ServiceDefinition(String name, List<String> requiredDocuments) {
    }
}
