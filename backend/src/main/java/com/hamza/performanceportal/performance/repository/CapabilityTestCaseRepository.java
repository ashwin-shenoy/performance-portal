package com.hamza.performanceportal.performance.repository;

import com.hamza.performanceportal.performance.entity.CapabilityTestCase;
import com.hamza.performanceportal.performance.entity.Capability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapabilityTestCaseRepository extends JpaRepository<CapabilityTestCase, Long> {

    List<CapabilityTestCase> findByCapabilityId(Long capabilityId);

    List<CapabilityTestCase> findByCapabilityIdOrderByPriorityAsc(Long capabilityId);

    /**
     * Find test cases by capability and test case names
     */
    List<CapabilityTestCase> findByCapabilityAndTestCaseNameIn(Capability capability, List<String> testCaseNames);

    void deleteByCapabilityId(Long capabilityId);
}
