package com.hamza.performanceportal.performance.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * TestTransaction entity representing individual transaction records
 */
@Entity
@Table(name = "test_transactions", indexes = {
    @Index(name = "idx_transaction_test_run", columnList = "test_run_id"),
    @Index(name = "idx_transaction_name", columnList = "transaction_name"),
    @Index(name = "idx_transaction_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private TestRun testRun;

    @Column(name = "transaction_name", nullable = false, length = 200)
    private String transactionName;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "response_time", nullable = false)
    private Long responseTime;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "thread_name", length = 100)
    private String threadName;

    @Column(name = "bytes_sent")
    private Long bytesSent;

    @Column(name = "bytes_received")
    private Long bytesReceived;

    @Column(name = "latency")
    private Long latency;

    @Column(name = "connect_time")
    private Long connectTime;
}

// Made with Bob
