package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LocalizedTemplateEntity;

/**
 * {@link LocalizedTemplateEntity} 测试用固定样例
 */
public final class LocalizedTemplateEntityFixtures {

    private LocalizedTemplateEntityFixtures() {
    }

    public static LocalizedTemplateEntity smsCaptchaZhCn(String templateId) {
        LocalizedTemplateEntity entity = new LocalizedTemplateEntity();
        entity.setTemplateId(templateId);
        entity.setLanguage(Language.ZH_CN);
        entity.setTitle("验证码");
        entity.setContent("您好 {{name}}，您的验证码是 {{code}}");
        return entity;
    }

    public static LocalizedTemplateEntity greetEn(String templateId) {
        LocalizedTemplateEntity entity = new LocalizedTemplateEntity();
        entity.setTemplateId(templateId);
        entity.setLanguage(Language.EN);
        entity.setTitle("t");
        entity.setContent("Hello {{name}}");
        return entity;
    }

    public static LocalizedTemplateEntity emptyBodyDe(String templateId) {
        LocalizedTemplateEntity entity = new LocalizedTemplateEntity();
        entity.setTemplateId(templateId);
        entity.setLanguage(Language.DE);
        entity.setTitle("only title");
        entity.setContent(null);
        return entity;
    }

    public static LocalizedTemplateEntity invZhCn(String templateId) {
        LocalizedTemplateEntity entity = new LocalizedTemplateEntity();
        entity.setTemplateId(templateId);
        entity.setLanguage(Language.ZH_CN);
        entity.setTitle("t");
        entity.setContent("x {{p}}");
        return entity;
    }
}