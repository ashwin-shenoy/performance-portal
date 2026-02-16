package com.hamza.durandhar.performance.repository;

import com.hamza.durandhar.performance.entity.Capability;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Capability entity
 */
@Repository
public interface CapabilityRepository extends JpaRepository<Capability, Long> {
    
    /**
     * Find capability by name
     */
    Optional<Capability> findByName(String name);
    
    /**
     * Find all active capabilities
     */
    List<Capability> findByIsActiveTrue();
    
    /**
     * Check if capability exists by name
     */
    boolean existsByName(String name);

    /**
     * Count test cases per capability (includes zero-count capabilities)
     */
    @Query("select c.id, count(tc) from Capability c left join c.testCases tc group by c.id")
    List<Object[]> fetchTestCaseCounts();
}

// Made with Bob