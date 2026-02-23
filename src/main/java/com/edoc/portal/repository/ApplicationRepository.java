package com.edoc.portal.repository;

import com.edoc.portal.entity.Application;
import com.edoc.portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByCustomerOrderByAppliedDateDesc(User customer);

    List<Application> findAllByOrderByAppliedDateDesc();
}
