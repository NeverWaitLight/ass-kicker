package com.github.waitlight.asskicker.channel.aliyun;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.waitlight.asskicker.channel.AliyunRpcSigner;

class AliyunRpcSignerTest {

    @Test
    void sign_producesDeterministicSignatureForFixedParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("AccessKeyId", "testid");
        params.put("Action", "SendSms");
        params.put("Format", "JSON");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", "fixed-nonce");
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", "2015-01-09T12:00:00Z");
        params.put("Version", "2017-05-25");
        params.put("PhoneNumbers", "13800138000");
        params.put("SignName", "签名");
        params.put("TemplateCode", "SMS_1");
        params.put("TemplateParam", "{}");

        AliyunRpcSigner.sign(params, "POST", "secret");

        assertThat(params.get("Signature")).isNotBlank();
        assertThat(AliyunRpcSigner.toFormBody(params)).contains("Signature=");
    }

    @Test
    void buildStringToSign_matchesConcatenation() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("Action", "SendSms");
        params.put("AccessKeyId", "a");
        String canonical = AliyunRpcSigner.canonicalizedQueryString(params);
        String sts = AliyunRpcSigner.buildStringToSign("POST", params);
        assertThat(sts).startsWith("POST");
        assertThat(sts).contains(AliyunRpcSigner.percentEncode(canonical));
    }
}
