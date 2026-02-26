package com.github.waitlight.asskicker.sender.im;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 钉钉机器人签名加密工具类
 */
public class DingTalkSignature {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * 生成钉钉机器人签名
     *
     * @param secret 密钥
     * @param timestamp 时间戳（毫秒）
     * @return URL 编码后的签名
     * @throws RuntimeException 签名生成失败
     */
    public static String generateSignature(String secret, long timestamp) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(signData);
            return URLEncoder.encode(sign, StandardCharsets.UTF_8.name());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("生成钉钉签名失败", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("URL 编码失败", e);
        }
    }

    /**
     * 构建带签名的 webhook URL
     *
     * @param webhookUrl 原始 webhook URL
     * @param secret 密钥
     * @return 带签名和时间戳的完整 URL
     */
    public static String buildSignedWebhookUrl(String webhookUrl, String secret) {
        if (secret == null || secret.isEmpty()) {
            return webhookUrl;
        }

        long timestamp = System.currentTimeMillis();
        String signature = generateSignature(secret, timestamp);
        return webhookUrl + "&timestamp=" + timestamp + "&sign=" + signature;
    }
}
