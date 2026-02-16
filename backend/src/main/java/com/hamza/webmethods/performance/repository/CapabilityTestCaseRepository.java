package com.hamza.durandhar.performance.repository;

import com.hamza.durandhar.performance.entity.CapabilityTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapabilityTestCaseRepository extends JpaRepository<CapabilityTestCase, Long> {

    List<CapabilityTestCase> findByCapabilityId(Long capabilityId);

    List<CapabilityTestCase> findByCapabilityIdOrderByPriorityAsc(Long capabilityId);

    void deleteByCapabilityId(Long capabilityId);
}
