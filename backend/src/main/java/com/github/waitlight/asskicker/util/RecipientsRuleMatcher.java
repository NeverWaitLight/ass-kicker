package com.github.waitlight.asskicker.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Include regex takes precedence over exclude: if include is set, the recipient must match it
 * (exclude is ignored). If include is unset, recipient must not match exclude when exclude is set.
 */
public final class RecipientsRuleMatcher {

    private RecipientsRuleMatcher() {
    }

    /**
     * @throws PatternSyntaxException if regex is non-blank and invalid
     */
    public static void validatePatternSyntax(String regex) throws PatternSyntaxException {
        if (normalizeBlankToNull(regex) == null) {
            return;
        }
        Pattern.compile(regex.trim());
    }

    public static boolean isAllowed(String recipient, String includeRecipientRegex, String excludeRecipientRegex) {
        String include = normalizeBlankToNull(includeRecipientRegex);
        String exclude = normalizeBlankToNull(excludeRecipientRegex);
        String value = recipient == null ? "" : recipient;
        if (include != null) {
            return Pattern.compile(include).matcher(value).matches();
        }
        if (exclude != null) {
            return !Pattern.compile(exclude).matcher(value).matches();
        }
        return true;
    }

    private static String normalizeBlankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
