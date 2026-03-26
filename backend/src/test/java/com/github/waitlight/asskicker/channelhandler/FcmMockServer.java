package com.github.waitlight.asskicker.channelhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FCM Mock Server for testing FcmChannelHandler.
 *
 * <p>
 * Uses OkHttp3 MockWebServer to simulate Firebase Cloud Messaging (FCM)
 * HTTP v1 API.
 * Supports success and error scenarios (400, 401, 404, 500).
 *
 * <p>
 * Usage:
 *
 * <pre>
 * FcmMockServer mockServer = new FcmMockServer("my-project-id");
 * mockServer.start();
 * try {
 *     mockServer.enqueueSuccess("msg-id-123");
 *     // ... perform test ...
 *     RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
 *     mockServer.verifyRequestHeaders(request);
 * } finally {
 *     mockServer.shutdown();
 * }
 * </pre>
 */
class FcmMockServer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MockWebServer server;
    private final String projectId;

    FcmMockServer(String projectId) {
        this.server = new MockWebServer();
        this.projectId = projectId;
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
     * Use this to configure FcmChannelHandler's url property.
     *
     * @return Base URL in format "http://localhost:PORT"
     */
    String getBaseUrl() {
        return server.url("/").toString().replaceAll("/$", "");
    }

    /**
     * Enqueues a successful FCM response (200 OK).
     *
     * @param messageId The message ID to return in response body (auto-generated if null)
     */
    void enqueueSuccess(String messageId) throws Exception {
        String id = messageId != null ? messageId : UUID.randomUUID().toString();
        String name = "projects/" + projectId + "/messages/" + id;
        String body = OBJECT_MAPPER.writeValueAsString(Map.of("name", name));
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Enqueues a Bad Request response (400).
     *
     * @param error The error status string (e.g., "INVALID_ARGUMENT")
     */
    void enqueueBadRequest(String error) throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(
                Map.of("error", Map.of("status", error, "message", "Bad request")));
        MockResponse response = new MockResponse()
                .setResponseCode(400)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Enqueues an Unauthorized response (401).
     * Simulates invalid or expired OAuth2 access token.
     */
    void enqueueUnauthorized() throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(
                Map.of("error", Map.of("status", "UNAUTHENTICATED", "message", "Invalid access token")));
        MockResponse response = new MockResponse()
                .setResponseCode(401)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Enqueues a Not Found response (404).
     * Simulates invalid device token or project not found.
     */
    void enqueueNotFound() throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(
                Map.of("error", Map.of("status", "NOT_FOUND", "message", "Registration token not found")));
        MockResponse response = new MockResponse()
                .setResponseCode(404)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        server.enqueue(response);
    }

    /**
     * Enqueues an Internal Server Error response (500).
     * Simulates FCM server error.
     */
    void enqueueServerError() throws Exception {
        String body = OBJECT_MAPPER.writeValueAsString(
                Map.of("error", Map.of("status", "INTERNAL", "message", "Internal server error")));
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
     * Verifies that the request contains required FCM headers.
     *
     * @param request The recorded request to verify
     * @throws AssertionError if required headers are missing or invalid
     */
    void verifyRequestHeaders(RecordedRequest request) {
        if (request == null) {
            throw new AssertionError("Request is null");
        }

        if (!"POST".equals(request.getMethod())) {
            throw new AssertionError("Expected POST method, got: " + request.getMethod());
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AssertionError("Missing or invalid Authorization header: " + authorization);
        }

        String contentType = request.getHeader("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            throw new AssertionError("Expected Content-Type=application/json, got: " + contentType);
        }
    }

    /**
     * Verifies that the request body contains the expected FCM message structure.
     *
     * @param request       The recorded request to verify
     * @param expectedToken Expected device token (null to skip check)
     * @param expectedTitle Expected notification title (null to skip check)
     * @param expectedBody  Expected notification body
     * @throws AssertionError if payload structure is invalid
     */
    void verifyRequestBody(RecordedRequest request, String expectedToken, String expectedTitle,
                           String expectedBody) throws Exception {
        if (request == null) {
            throw new AssertionError("Request is null");
        }

        String bodyString = request.getBody().readUtf8();
        if (bodyString == null || bodyString.isBlank()) {
            throw new AssertionError("Request body is empty");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = OBJECT_MAPPER.readValue(bodyString, Map.class);

        if (!payload.containsKey("message")) {
            throw new AssertionError("Payload missing 'message' key");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) payload.get("message");

        if (expectedToken != null) {
            Object actualToken = message.get("token");
            if (!expectedToken.equals(actualToken)) {
                throw new AssertionError(
                        "Expected token='" + expectedToken + "', got: '" + actualToken + "'");
            }
        }

        if (!message.containsKey("notification")) {
            throw new AssertionError("Message missing 'notification' key");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> notification = (Map<String, Object>) message.get("notification");

        if (expectedTitle != null) {
            Object actualTitle = notification.get("title");
            if (!expectedTitle.equals(actualTitle)) {
                throw new AssertionError(
                        "Expected title='" + expectedTitle + "', got: '" + actualTitle + "'");
            }
        }

        Object actualBody = notification.get("body");
        if (!expectedBody.equals(actualBody)) {
            throw new AssertionError(
                    "Expected body='" + expectedBody + "', got: '" + actualBody + "'");
        }

        if (!message.containsKey("android")) {
            throw new AssertionError("Message missing 'android' key");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> android = (Map<String, Object>) message.get("android");
        if (!"HIGH".equals(android.get("priority"))) {
            throw new AssertionError("Expected android.priority=HIGH, got: " + android.get("priority"));
        }
    }

    /**
     * Gets the number of requests received by the server.
     */
    int getRequestCount() {
        return server.getRequestCount();
    }
}
