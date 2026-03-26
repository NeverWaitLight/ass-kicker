package com.github.waitlight.asskicker.channelhandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * APNs Mock Server for testing ApnsChannelHandler.
 *
 * <p>
 * Uses OkHttp3 MockWebServer to simulate Apple Push Notification service (APNs)
 * HTTP/2 API.
 * Supports success and error scenarios (400, 401, 410, 500).
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * ApnsMockServer mockServer = new ApnsMockServer();
 * mockServer.start();
 * try {
 *     mockServer.enqueueSuccess("apns-id-123");
 *     // ... perform test ...
 *     RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
 *     mockServer.verifyRequestHeaders(request);
 * } finally {
 *     mockServer.shutdown();
 * }
 * </pre>
 */
class ApnsMockServer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MockWebServer server;

    ApnsMockServer() {
        this.server = new MockWebServer();
    }

    /**
     * Starts the mock server on a random available port.
     */
    void start() throws IOException {
        server.start();
    }

    /**
     * Shuts down the mock server and releases resources.
     */
    void shutdown() throws IOException {
        server.shutdown();
    }

    /**
     * Gets the base URL of the mock server (without trailing slash).
     * Use this to configure ApnsChannelHandler's endpoint.
     *
     * @return Base URL in format "http://localhost:PORT/3/device"
     */
    String getBaseUrl() {
        return server.url("/3/device").toString().replaceAll("/$", "");
    }

    /**
     * Enqueues a successful APNs response (200 OK).
     *
     * @param apnsId The apns-id to return in response header (auto-generated if
     *               null)
     */
    void enqueueSuccess(String apnsId) {
        String id = apnsId != null ? apnsId : UUID.randomUUID().toString();
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .addHeader("apns-id", id);
        server.enqueue(response);
    }

    /**
     * Enqueues a Bad Request response (400).
     *
     * @param reason The error reason (e.g., "BadMessageId", "PayloadTooLarge")
     */
    void enqueueBadRequest(String reason) throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(Map.of("reason", reason));
        MockResponse response = new MockResponse()
                .setResponseCode(400)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Enqueues an Unauthorized response (401).
     * Simulates JWT authentication failure.
     */
    void enqueueUnauthorized() throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(Map.of("reason", "InvalidProviderToken"));
        MockResponse response = new MockResponse()
                .setResponseCode(401)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Enqueues a Gone response (410).
     * Simulates invalid or expired device token.
     */
    void enqueueGoneDeviceToken() throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(Map.of("reason", "BadDeviceToken"));
        MockResponse response = new MockResponse()
                .setResponseCode(410)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Enqueues an Internal Server Error response (500).
     * Simulates APNs server error.
     */
    void enqueueServerError() throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(Map.of("reason", "InternalServerError"));
        MockResponse response = new MockResponse()
                .setResponseCode(500)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Retrieves the next recorded request from the server.
     *
     * @param timeout Maximum time to wait
     * @param unit    Time unit
     * @return The recorded request, or null if timeout
     */
    RecordedRequest takeRequest(long timeout, TimeUnit unit) throws InterruptedException {
        return server.takeRequest(timeout, unit);
    }

    /**
     * Verifies that the request contains required APNs headers.
     *
     * @param request The recorded request to verify
     * @throws AssertionError if required headers are missing or invalid
     */
    void verifyRequestHeaders(RecordedRequest request) {
        if (request == null) {
            throw new AssertionError("Request is null");
        }

        // Verify method
        if (!"POST".equals(request.getMethod())) {
            throw new AssertionError("Expected POST method, got: " + request.getMethod());
        }

        // Verify required headers
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("bearer ")) {
            throw new AssertionError("Missing or invalid Authorization header: " + authorization);
        }

        String apnsTopic = request.getHeader("apns-topic");
        if (apnsTopic == null || apnsTopic.isBlank()) {
            throw new AssertionError("Missing apns-topic header");
        }

        String apnsPushType = request.getHeader("apns-push-type");
        if (!"alert".equals(apnsPushType)) {
            throw new AssertionError("Expected apns-push-type=alert, got: " + apnsPushType);
        }

        String apnsPriority = request.getHeader("apns-priority");
        if (!"10".equals(apnsPriority)) {
            throw new AssertionError("Expected apns-priority=10, got: " + apnsPriority);
        }

        String contentType = request.getHeader("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            throw new AssertionError("Expected Content-Type=application/json, got: " + contentType);
        }
    }

    /**
     * Verifies that the request body contains the expected APNs payload structure.
     *
     * @param request       The recorded request to verify
     * @param expectedTitle Expected alert title (null to skip check)
     * @param expectedBody  Expected alert body
     * @throws AssertionError if payload structure is invalid
     */
    void verifyRequestBody(RecordedRequest request, String expectedTitle, String expectedBody) throws Exception {
        if (request == null) {
            throw new AssertionError("Request is null");
        }

        String bodyString = request.getBody().readUtf8();
        if (bodyString == null || bodyString.isBlank()) {
            throw new AssertionError("Request body is empty");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = OBJECT_MAPPER.readValue(bodyString, Map.class);

        // Verify "aps" key exists
        if (!payload.containsKey("aps")) {
            throw new AssertionError("Payload missing 'aps' key");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> aps = (Map<String, Object>) payload.get("aps");

        // Verify "alert" exists
        if (!aps.containsKey("alert")) {
            throw new AssertionError("Payload 'aps' missing 'alert' key");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

        // Verify title (if expected)
        if (expectedTitle != null) {
            Object actualTitle = alert.get("title");
            if (!expectedTitle.equals(actualTitle)) {
                throw new AssertionError(
                        "Expected title='" + expectedTitle + "', got: '" + actualTitle + "'");
            }
        }

        // Verify body
        Object actualBody = alert.get("body");
        if (!expectedBody.equals(actualBody)) {
            throw new AssertionError(
                    "Expected body='" + expectedBody + "', got: '" + actualBody + "'");
        }

        // Verify sound
        if (!"default".equals(aps.get("sound"))) {
            throw new AssertionError("Expected sound='default', got: " + aps.get("sound"));
        }
    }

    /**
     * Gets the number of requests received by the server.
     */
    int getRequestCount() {
        return server.getRequestCount();
    }
}
