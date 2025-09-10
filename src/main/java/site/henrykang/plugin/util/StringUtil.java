package site.henrykang.plugin.util;

import org.jetbrains.annotations.NotNull;

public class StringUtil {

    public static String trimSlashes(@NotNull String input) {
        return input.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public static String extractDomainPart(String packageName) {
        if (isBlank(packageName)) return "";
        String[] parts = packageName.split("\\.");
        if (parts.length < 2) return packageName;
        return parts[0] + "." + parts[1];
    }

    public static boolean isBlank(CharSequence cs) {
        final int strLen = cs == null ? 0 : cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.isEmpty();
    }

    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

}
