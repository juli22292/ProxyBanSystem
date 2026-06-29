package proxyBanSystem;

import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-ORX]");

    private ColorUtil() {
    }

    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }

        return LEGACY_COLOR_PATTERN.matcher(text).replaceAll("");
    }
}
