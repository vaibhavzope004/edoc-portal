package com.edoc.portal.repository;

import com.edoc.portal.entity.CscUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CscUserProfileRepository extends JpaRepository<CscUserProfile, Long> {

    boolean existsByCscId(String cscId);

    boolean existsByMobileNumber(String mobileNumber);
}
