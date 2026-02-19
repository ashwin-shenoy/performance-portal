package com.hamza.performanceportal.performance.service;

import com.hamza.performanceportal.performance.data.DocumentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Factory service for creating DocumentData instances based on capability names
 * Maps capability names to their corresponding data template classes
 */
@Service
@Slf4j
public class ReportDataFactory {

    /**
     * Get DocumentData template for a capability
     *
     * @param capabilityName Name of the capability (case-insensitive)
     * @return DocumentData instance for the capability, or null if not found
     */
    public DocumentData getDocumentDataForCapability(String capabilityName) {
        if (capabilityName == null || capabilityName.trim().isEmpty()) {
            log.warn("Capability name is null or empty, cannot create document data");
            return null;
        }

        log.info("Creating DocumentData for capability: {}", capabilityName);
        DocumentData data = new DocumentData();
        data.setCapabilityName(capabilityName);
        return data;
    }

    /**
     * Check if a capability is supported
     *
     * @param capabilityName Name of the capability
     * @return true if supported, false otherwise
     */
    public boolean isCapabilitySupported(String capabilityName) {
        return capabilityName != null && !capabilityName.trim().isEmpty();
    }

    /**
     * Get all supported capability names
     *
     * @return Array of supported capability names
     */
    public String[] getSupportedCapabilities() {
        return new String[0];
    }
}

// Made with Bob
