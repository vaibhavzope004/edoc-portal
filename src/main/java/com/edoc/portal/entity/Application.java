package com.edoc.portal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicantName;
    private String applicantMobile;
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String documentPath;

    private String issuedDocumentPath;

    private String issuedDocumentName;

    private String issuedDocumentContentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] issuedDocumentData;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @OneToMany(mappedBy = "application", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ApplicationDocument> uploadedDocumentEntities = new ArrayList<>();

    private LocalDateTime appliedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantMobile() {
        return applicantMobile;
    }

    public void setApplicantMobile(String applicantMobile) {
        this.applicantMobile = applicantMobile;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getIssuedDocumentPath() {
        return issuedDocumentPath;
    }

    public void setIssuedDocumentPath(String issuedDocumentPath) {
        this.issuedDocumentPath = issuedDocumentPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIssuedDocumentName() {
        return issuedDocumentName;
    }

    public void setIssuedDocumentName(String issuedDocumentName) {
        this.issuedDocumentName = issuedDocumentName;
    }

    public String getIssuedDocumentContentType() {
        return issuedDocumentContentType;
    }

    public void setIssuedDocumentContentType(String issuedDocumentContentType) {
        this.issuedDocumentContentType = issuedDocumentContentType;
    }

    public byte[] getIssuedDocumentData() {
        return issuedDocumentData;
    }

    public void setIssuedDocumentData(byte[] issuedDocumentData) {
        this.issuedDocumentData = issuedDocumentData;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDateTime appliedDate) {
        this.appliedDate = appliedDate;
    }

    public List<ApplicationDocument> getUploadedDocumentEntities() {
        if (uploadedDocumentEntities == null) {
            uploadedDocumentEntities = new ArrayList<>();
        }
        return uploadedDocumentEntities;
    }

    public void setUploadedDocumentEntities(List<ApplicationDocument> uploadedDocumentEntities) {
        this.uploadedDocumentEntities = uploadedDocumentEntities == null ? new ArrayList<>() : uploadedDocumentEntities;
    }

    @Transient
    public boolean isIssuedDocumentAvailable() {
        return (issuedDocumentData != null && issuedDocumentData.length > 0)
                || (issuedDocumentPath != null && !issuedDocumentPath.isBlank());
    }

    @Transient
    public boolean isSuccessLikeStatus() {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "SUCCESS".equals(normalized) || "APPROVED".equals(normalized) || "ISSUED".equals(normalized);
    }

    @Transient
    public boolean isIssuedDownloadAllowed() {
        return isSuccessLikeStatus() && isIssuedDocumentAvailable();
    }

    @Transient
    public String getName() {
        return applicantName;
    }

    @Transient
    public String getCustomerName() {
        if (customer != null && customer.getName() != null && !customer.getName().isBlank()) {
            return customer.getName();
        }
        return applicantName;
    }

    @Transient
    public String getCustomerEmail() {
        return customer != null ? customer.getEmail() : null;
    }

    @Transient
    public String getMobile() {
        return applicantMobile;
    }

    @Transient
    public String getServiceName() {
        return serviceType;
    }

    @Transient
    public LocalDateTime getCreatedAt() {
        return appliedDate;
    }

    @Transient
    public LocalDateTime getUpdatedAt() {
        return appliedDate;
    }

    @Transient
    public LocalDateTime getSubmittedDate() {
        return appliedDate;
    }

    @Transient
    public LocalDateTime getApprovedDate() {
        return appliedDate;
    }

    @Transient
    public String getRejectionReason() {
        return message;
    }

    @Transient
    public List<UploadedDocumentInfo> getUploadedDocuments() {
        List<UploadedDocumentInfo> docs = new ArrayList<>();
        List<ApplicationDocument> dbDocs = getUploadedDocumentEntities();
        if (!dbDocs.isEmpty()) {
            long baseId = (id == null ? 0L : id) * 1000L;
            for (int i = 0; i < dbDocs.size(); i++) {
                ApplicationDocument dbDoc = dbDocs.get(i);
                String docType = dbDoc.getDocumentType() == null || dbDoc.getDocumentType().isBlank()
                        ? "Document"
                        : dbDoc.getDocumentType();
                String fileName = dbDoc.getFileName() == null || dbDoc.getFileName().isBlank()
                        ? "document"
                        : dbDoc.getFileName();
                docs.add(new UploadedDocumentInfo(baseId + i + 1, docType, fileName, null));
            }
            return docs;
        }

        if (documentPath == null || documentPath.isBlank()) {
            return docs;
        }

        List<String> items;
        if (documentPath.contains("||")) {
            items = Arrays.stream(documentPath.split("\\|\\|")).toList();
        } else if (documentPath.contains(",")) {
            items = Arrays.stream(documentPath.split(",")).toList();
        } else {
            items = List.of(documentPath);
        }

        int index = 0;
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String[] parts = item.split("::", 2);
            String docType = parts.length == 2 ? parts[0] : "Document";
            String filePath = parts.length == 2 ? parts[1] : parts[0];
            String fileName = filePath;
            int idx = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            if (idx >= 0 && idx < filePath.length() - 1) {
                fileName = filePath.substring(idx + 1);
            }
            long baseId = (id == null ? 0L : id) * 1000L;
            docs.add(new UploadedDocumentInfo(baseId + index + 1, docType, fileName, filePath));
            index++;
        }
        return docs;
    }

    public record UploadedDocumentInfo(Long id, String documentType, String fileName, String filePath) {
    }
}
