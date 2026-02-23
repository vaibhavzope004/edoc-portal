package com.edoc.portal.entity;

import com.edoc.portal.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String status;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private AdminProfile adminProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private CscUserProfile cscProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomerProfile customerProfile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AdminProfile getAdminProfile() {
        return adminProfile;
    }

    public CscUserProfile getCscProfile() {
        return cscProfile;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    @Transient
    public String getFullName() {
        if (customerProfile != null && customerProfile.getFullName() != null && !customerProfile.getFullName().isBlank()) {
            return customerProfile.getFullName();
        }
        return name;
    }

    public void setFullName(String fullName) {
        this.name = fullName;
        ensureCustomerProfile().setFullName(fullName);
    }

    @Transient
    public String getMobile() {
        return getMobileNumber();
    }

    @Transient
    public String getMobileNumber() {
        if (customerProfile != null && customerProfile.getMobileNumber() != null && !customerProfile.getMobileNumber().isBlank()) {
            return customerProfile.getMobileNumber();
        }
        if (cscProfile != null && cscProfile.getMobileNumber() != null && !cscProfile.getMobileNumber().isBlank()) {
            return cscProfile.getMobileNumber();
        }
        return null;
    }

    public void setMobileNumber(String mobileNumber) {
        if (role == Role.CSC) {
            ensureCscProfile().setMobileNumber(mobileNumber);
            return;
        }
        if (role == Role.CUSTOMER) {
            ensureCustomerProfile().setMobileNumber(mobileNumber);
        }
    }

    @Transient
    public String getOwnerName() {
        if (cscProfile != null && cscProfile.getOwnerName() != null && !cscProfile.getOwnerName().isBlank()) {
            return cscProfile.getOwnerName();
        }
        return name;
    }

    public void setOwnerName(String ownerName) {
        ensureCscProfile().setOwnerName(ownerName);
        this.name = ownerName;
    }

    @Transient
    public String getUsernameEmail() {
        return email;
    }

    public void setUsernameEmail(String usernameEmail) {
        this.email = usernameEmail;
    }

    @Transient
    public String getAssignedCscEmail() {
        return customerProfile != null ? customerProfile.getAssignedCscEmail() : null;
    }

    public void setAssignedCscEmail(String assignedCscEmail) {
        ensureCustomerProfile().setAssignedCscEmail(assignedCscEmail);
    }

    @Transient
    public String getCscPortalName() {
        return cscProfile != null ? cscProfile.getCscPortalName() : null;
    }

    public void setCscPortalName(String cscPortalName) {
        ensureCscProfile().setCscPortalName(cscPortalName);
    }

    @Transient
    public String getCscId() {
        return cscProfile != null ? cscProfile.getCscId() : null;
    }

    public void setCscId(String cscId) {
        ensureCscProfile().setCscId(cscId);
    }

    @Transient
    public String getCscCenterAddress() {
        return cscProfile != null ? cscProfile.getCscCenterAddress() : null;
    }

    public void setCscCenterAddress(String cscCenterAddress) {
        ensureCscProfile().setCscCenterAddress(cscCenterAddress);
    }

    public void setAdminProfile(AdminProfile adminProfile) {
        this.adminProfile = adminProfile;
        if (adminProfile != null && adminProfile.getUser() != this) {
            adminProfile.setUser(this);
        }
    }

    public void setCscProfile(CscUserProfile cscProfile) {
        this.cscProfile = cscProfile;
        if (cscProfile != null && cscProfile.getUser() != this) {
            cscProfile.setUser(this);
        }
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
        if (customerProfile != null && customerProfile.getUser() != this) {
            customerProfile.setUser(this);
        }
    }

    private CscUserProfile ensureCscProfile() {
        if (cscProfile == null) {
            CscUserProfile profile = new CscUserProfile();
            setCscProfile(profile);
        }
        return cscProfile;
    }

    private CustomerProfile ensureCustomerProfile() {
        if (customerProfile == null) {
            CustomerProfile profile = new CustomerProfile();
            setCustomerProfile(profile);
        }
        return customerProfile;
    }
}
