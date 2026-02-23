package com.edoc.portal.repository;

import com.edoc.portal.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    boolean existsByMobileNumber(String mobileNumber);
}
