package com.hamza.performanceportal.performance.service;

import com.hamza.performanceportal.performance.entity.Capability;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstanaMetricsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.instana.enabled:false}")
    private boolean instanaEnabled;

    @Value("${app.instana.base-url:}")
    private String instanaBaseUrl;

    @Value("${app.instana.api-token:}")
    private String instanaApiToken;

    @Value("${app.instana.timeout-seconds:20}")
    private int timeoutSeconds;

    public InstanaSummary fetchSummary(Capability capability, LocalDateTime startTime, LocalDateTime endTime) {
        InstanaSummary empty = new InstanaSummary();

        if (!instanaEnabled) {
            log.info("Instana integration disabled; skipping telemetry fetch");
            return empty;
        }

        if (capability == null || capability.getInstanaConfig() == null || capability.getInstanaConfig().isEmpty()) {
            log.info("No Instana config found for capability; skipping telemetry fetch");
            return empty;
        }

        if (!StringUtils.hasText(instanaBaseUrl) || !StringUtils.hasText(instanaApiToken)) {
            log.warn("Instana base URL or API token missing; skipping telemetry fetch");
            return empty;
        }

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            log.warn("Invalid telemetry window start/end; skipping Instana fetch");
            return empty;
        }

        try {
            List<Double> podCpuValues = queryMetricValues(capability, "podCpuMetric", startTime, endTime);
            List<Double> podMemoryValues = queryMetricValues(capability, "podMemoryMetric", startTime, endTime);
            List<Double> heapValues = queryMetricValues(capability, "jvmHeapUsedMetric", startTime, endTime);
            List<Double> gcPauseValues = queryMetricValues(capability, "jvmGcPauseMetric", startTime, endTime);
            List<Double> processCpuValues = queryMetricValues(capability, "jvmProcessCpuMetric", startTime, endTime);

            InstanaSummary summary = new InstanaSummary();
            summary.setPodCpuAvg(average(podCpuValues));
            summary.setPodCpuMax(max(podCpuValues));
            summary.setPodMemoryAvg(average(podMemoryValues));
            summary.setPodMemoryMax(max(podMemoryValues));
            summary.setJvmHeapUsedPercentAvg(average(heapValues));
            summary.setJvmGcPauseMsP95(percentile(gcPauseValues, 95));
            summary.setJvmProcessCpuAvg(average(processCpuValues));
            return summary;
        } catch (Exception e) {
            log.error("Failed to fetch Instana metrics summary: {}", e.getMessage(), e);
            return empty;
        }
    }

    private List<Double> queryMetricValues(
            Capability capability,
            String metricConfigKey,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        Object metricNameObj = capability.getInstanaConfig().get(metricConfigKey);
        if (!(metricNameObj instanceof String metricName) || !StringUtils.hasText(metricName)) {
            return List.of();
        }

        String namespace = asString(capability.getInstanaConfig().get("namespace"));
        String entityType = asString(capability.getInstanaConfig().get("entityType"));
        String query = asString(capability.getInstanaConfig().get("query"));

        long fromMs = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long toMs = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        StringBuilder url = new StringBuilder();
        url.append(instanaBaseUrl)
                .append("/api/metrics/query")
                .append("?metric=").append(urlEncode(metricName))
                .append("&from=").append(fromMs)
                .append("&to=").append(toMs);

        if (StringUtils.hasText(namespace)) {
            url.append("&namespace=").append(urlEncode(namespace));
        }
        if (StringUtils.hasText(entityType)) {
            url.append("&entityType=").append(urlEncode(entityType));
        }
        if (StringUtils.hasText(query)) {
            url.append("&query=").append(urlEncode(query));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "apiToken " + instanaApiToken);
        headers.add("Accept", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        long startedAt = System.currentTimeMillis();
        ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.GET, request, Map.class);
        long elapsedMs = System.currentTimeMillis() - startedAt;
        if (elapsedMs > Duration.ofSeconds(timeoutSeconds).toMillis()) {
            log.warn("Instana request exceeded configured timeout hint ({} ms)", elapsedMs);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return List.of();
        }

        return extractValues(response.getBody());
    }

    private List<Double> extractValues(Map<?, ?> payload) {
        Object itemsObj = payload.get("items");
        if (!(itemsObj instanceof List<?> items)) {
            return List.of();
        }

        List<Double> values = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                continue;
            }

            Object pointsObj = itemMap.get("data");
            if (!(pointsObj instanceof List<?> points)) {
                continue;
            }

            for (Object point : points) {
                if (point instanceof List<?> tuple && tuple.size() >= 2) {
                    Object valueObj = tuple.get(1);
                    Double number = toDouble(valueObj);
                    if (number != null) {
                        values.add(number);
                    }
                    continue;
                }

                if (point instanceof Map<?, ?> pointMap) {
                    Double number = toDouble(pointMap.get("value"));
                    if (number != null) {
                        values.add(number);
                    }
                }
            }
        }

        return values;
    }

    private Double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Double max(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().max(Double::compareTo).orElse(null);
    }

    private Double percentile(List<Double> values, int percentile) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String urlEncode(String value) {
        return value.replace(" ", "%20");
    }

    @Data
    public static class InstanaSummary {
        private Double podCpuAvg;
        private Double podCpuMax;
        private Double podMemoryAvg;
        private Double podMemoryMax;
        private Double jvmHeapUsedPercentAvg;
        private Double jvmGcPauseMsP95;
        private Double jvmProcessCpuAvg;
    }
}
