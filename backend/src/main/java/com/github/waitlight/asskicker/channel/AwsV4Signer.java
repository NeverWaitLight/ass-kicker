package com.github.waitlight.asskicker.channel;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * AWS Signature Version 4（用于 SNS 等 Query POST），不依赖 AWS SDK。
 */
public final class AwsV4Signer {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private AwsV4Signer() {
    }

    public record SignedRequest(String url, String body, Map<String, String> headers) {
    }

    /**
     * 对 SNS Query API 的 POST 请求签名。
     *
     * @param endpointUrlOverride 完整 base URL（含路径可为 /）
     */
    public static SignedRequest signSnsPublish(
            String region,
            String endpointUrlOverride,
            String accessKeyId,
            String secretAccessKey,
            String sessionToken,
            String phoneNumberE164,
            String message,
            Instant signingInstant) {

        if (StringUtils.isBlank(endpointUrlOverride)) {
            throw new IllegalArgumentException("AWS SNS endpointUrlOverride required");
        }
        String url = normalizeEndpoint(endpointUrlOverride.trim());
        URI uri = URI.create(url);
        String host = hostHeader(uri);
        String canonicalUri = uri.getPath();
        if (canonicalUri == null || canonicalUri.isEmpty()) {
            canonicalUri = "/";
        }

        TreeMap<String, String> params = new TreeMap<>();
        params.put("Action", "Publish");
        params.put("Message", message);
        params.put("PhoneNumber", phoneNumberE164);
        params.put("Version", "2010-03-31");

        String body = formEncodeUtf8(params);
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String payloadHash = sha256Hex(bodyBytes);

        String amzDate = AMZ_DATE.format(signingInstant);
        String dateStamp = DATE_STAMP.format(signingInstant);

        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("content-type", "application/x-www-form-urlencoded; charset=utf-8");
        headers.put("host", host);
        headers.put("x-amz-content-sha256", payloadHash);
        headers.put("x-amz-date", amzDate);
        if (sessionToken != null && !sessionToken.isBlank()) {
            headers.put("x-amz-security-token", sessionToken.trim());
        }

        String canonicalHeaders = headers.entrySet().stream()
                .map(e -> e.getKey().toLowerCase() + ":" + e.getValue().trim() + "\n")
                .collect(Collectors.joining());

        String signedHeaders = headers.keySet().stream()
                .map(String::toLowerCase)
                .sorted()
                .collect(Collectors.joining(";"));

        String canonicalRequest = "POST\n"
                + canonicalUri + "\n"
                + "\n"
                + canonicalHeaders
                + "\n"
                + signedHeaders
                + "\n"
                + payloadHash;

        String algorithm = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + region + "/sns/aws4_request";
        String stringToSign = algorithm + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, "sns");
        String signature = hmacHex(signingKey, stringToSign);

        String authorization = algorithm + " "
                + "Credential=" + accessKeyId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        TreeMap<String, String> outHeaders = new TreeMap<>();
        outHeaders.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        outHeaders.put("Host", host);
        outHeaders.put("X-Amz-Date", amzDate);
        outHeaders.put("X-Amz-Content-Sha256", payloadHash);
        outHeaders.put("Authorization", authorization);
        if (sessionToken != null && !sessionToken.isBlank()) {
            outHeaders.put("X-Amz-Security-Token", sessionToken.trim());
        }

        return new SignedRequest(url, body, outHeaders);
    }

    private static String normalizeEndpoint(String url) {
        if (url.endsWith("/")) {
            return url;
        }
        return url + "/";
    }

    static String hostHeader(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("invalid SNS endpoint URI: " + uri);
        }
        int port = uri.getPort();
        if (port > 0) {
            boolean defaultHttps = "https".equalsIgnoreCase(uri.getScheme()) && port == 443;
            boolean defaultHttp = "http".equalsIgnoreCase(uri.getScheme()) && port == 80;
            if (!defaultHttps && !defaultHttp) {
                return host + ":" + port;
            }
        }
        return host;
    }

    private static String formEncodeUtf8(TreeMap<String, String> sorted) {
        return sorted.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String urlEncode(String s) {
        if (s == null) {
            return "";
        }
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            return toHex(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] getSignatureKey(String secretKey, String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacRaw(kSecret, dateStamp);
        byte[] kRegion = hmacRaw(kDate, regionName);
        byte[] kService = hmacRaw(kRegion, serviceName);
        return hmacRaw(kService, "aws4_request");
    }

    private static byte[] hmacRaw(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hmacHex(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
