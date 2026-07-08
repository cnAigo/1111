package org.example.testvue.service;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ApiExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ApiExecutionService.class);
    private final RestTemplate restTemplate;

    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "localhost", "127.0.0.1", "0.0.0.0", "::1",
        "169.254.169.254", "metadata.google.internal",
        "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"
    );

    public ApiExecutionService() {
        this.restTemplate = createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return new RestTemplate(factory);
    }

    public Map<String, Object> execute(String method, String url, String headersJson,
                                        String paramsJson, String body, String bodyType) {
        Map<String, Object> result = new LinkedHashMap<>();
        Instant start = Instant.now();

        try {
            // Validate URL to prevent SSRF
            validateUrl(url);

            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            HttpHeaders headers = buildHeaders(headersJson);

            String fullUrl = buildUrl(url, paramsJson);

            HttpEntity<String> entity;
            if (body != null && !body.isEmpty() && !"none".equals(bodyType)) {
                MediaType contentType = getContentType(bodyType);
                headers.setContentType(contentType);
                entity = new HttpEntity<>(body, headers);
            } else {
                entity = new HttpEntity<>(headers);
            }

            ResponseEntity<String> response = restTemplate.exchange(fullUrl, httpMethod, entity, String.class);

            long ms = Duration.between(start, Instant.now()).toMillis();
            result.put("status_code", response.getStatusCode().value());
            result.put("response_time", ms + "ms");
            result.put("body", response.getBody());
            result.put("headers", response.getHeaders().toString());
        } catch (IllegalArgumentException e) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            result.put("status_code", 0);
            result.put("response_time", ms + "ms");
            result.put("body", "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            result.put("headers", "");
            log.warn("SSRF blocked: {} {} — {}", method, url, e.getMessage());
        } catch (Exception e) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            result.put("status_code", 0);
            result.put("response_time", ms + "ms");
            result.put("body", "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            result.put("headers", "");
            log.warn("Request failed: {} {} — {}", method, url, e.getMessage());
        }
        return result;
    }

    private void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL is empty");
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Invalid URL: no host");
            }

            // Check blocked hosts
            String lowerHost = host.toLowerCase();
            for (String blocked : BLOCKED_HOSTS) {
                if (lowerHost.equals(blocked) || lowerHost.contains(blocked)) {
                    throw new IllegalArgumentException("Access to internal address is blocked: " + host);
                }
            }

            // Resolve host to check for internal IPs
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
                throw new IllegalArgumentException("Access to internal network is blocked: " + host);
            }

            // Only allow http and https schemes
            String scheme = uri.getScheme();
            if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
            }
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + url);
        }
    }

    private HttpHeaders buildHeaders(String headersJson) {
        HttpHeaders headers = new HttpHeaders();
        if (headersJson != null && !headersJson.isEmpty()) {
            try {
                JsonArray arr = JsonParser.parseString(headersJson).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject h = el.getAsJsonObject();
                    String key = h.get("key").getAsString();
                    String value = h.get("value").getAsString();
                    if (key != null && !key.isEmpty()) {
                        headers.add(key, value);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse headers JSON: {}", e.getMessage());
            }
        }
        return headers;
    }

    private String buildUrl(String baseUrl, String paramsJson) {
        if (paramsJson == null || paramsJson.isEmpty()) return baseUrl;
        try {
            JsonArray arr = JsonParser.parseString(paramsJson).getAsJsonArray();
            StringBuilder sb = new StringBuilder(baseUrl);
            boolean first = !baseUrl.contains("?");
            for (JsonElement el : arr) {
                JsonObject p = el.getAsJsonObject();
                String key = p.get("key").getAsString();
                String value = p.get("value").getAsString();
                if (key != null && !key.isEmpty()) {
                    sb.append(first ? "?" : "&");
                    sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                    sb.append("=");
                    sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                    first = false;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to build URL params: {}", e.getMessage());
            return baseUrl;
        }
    }

    private MediaType getContentType(String bodyType) {
        return switch (bodyType) {
            case "form-data" -> MediaType.MULTIPART_FORM_DATA;
            case "x-www-form-urlencoded" -> MediaType.APPLICATION_FORM_URLENCODED;
            case "xml" -> MediaType.APPLICATION_XML;
            case "binary" -> MediaType.APPLICATION_OCTET_STREAM;
            default -> MediaType.APPLICATION_JSON;
        };
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
