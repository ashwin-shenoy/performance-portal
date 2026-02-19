package com.hamza.performanceportal.performance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for performance portal Performance Portal
 *
 * @author Hamza Bob
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {BatchAutoConfiguration.class})
@EnableAsync
@EnableScheduling
public class PerformancePortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformancePortalApplication.class, args);
    }
}

// Made with Bob
