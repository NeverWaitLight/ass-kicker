package com.github.waitlight.asskicker.channel.aliyun;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 阿里云 RPC 风格 API 签名（Signature Version 1.0，HMAC-SHA1），用于无 SDK 的 HTTP 调用。
 */
public final class AliyunRpcSigner {

    private static final String ALGORITHM = "HmacSHA1";

    private AliyunRpcSigner() {
    }

    /**
     * 计算 Signature 并放入参数表（会修改传入的 map）。
     */
    public static void sign(Map<String, String> params, String httpMethod, String accessKeySecret) {
        String stringToSign = buildStringToSign(httpMethod, params);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            params.put("Signature", Base64.getEncoder().encodeToString(raw));
        } catch (Exception e) {
            throw new IllegalStateException("Aliyun RPC sign failed", e);
        }
    }

    static String buildStringToSign(String httpMethod, Map<String, String> params) {
        String canonical = canonicalizedQueryString(params);
        return percentEncode(httpMethod) + "&" + percentEncode("/") + "&" + percentEncode(canonical);
    }

    static String canonicalizedQueryString(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if ("Signature".equalsIgnoreCase(e.getKey())) {
                continue;
            }
            sorted.put(e.getKey(), e.getValue() == null ? "" : e.getValue());
        }
        return joinSortedEncoded(sorted);
    }

    /**
     * 签名完成后，生成 POST 表单体（含 Signature，键名排序）。
     */
    public static String toFormBody(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            sorted.put(e.getKey(), e.getValue() == null ? "" : e.getValue());
        }
        return joinSortedEncoded(sorted);
    }

    private static String joinSortedEncoded(TreeMap<String, String> sorted) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(percentEncode(e.getKey())).append("=").append(percentEncode(e.getValue()));
        }
        return sb.toString();
    }

    /**
     * 与阿里云文档一致的百分号编码（UTF-8）。
     */
    public static String percentEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
