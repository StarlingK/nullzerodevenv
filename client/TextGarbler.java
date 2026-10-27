package nullmod.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.util.FormattedCharSequence;

public final class TextGarbler {
    private static final String NOISE = "#$%&*?@=+/<>[]{}~^|0123456789";

    private TextGarbler() {}

    public static boolean active() {
        return ClientState.glitching();
    }

    private static int scramble(int codePoint) {
        if (Character.isWhitespace(codePoint) || ThreadLocalRandom.current().nextFloat() > 0.3f) {
            return codePoint;
        }
        return NOISE.charAt(ThreadLocalRandom.current().nextInt(NOISE.length()));
    }

    public static String garble(String text) {
        if (!active() || text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            sb.appendCodePoint(cp == '§' ? cp : scramble(cp));
            if (cp == '§' && i < text.length()) {

                int next = text.codePointAt(i);
                sb.appendCodePoint(next);
                i += Character.charCount(next);
            }
        }
        return sb.toString();
    }

    public static FormattedCharSequence garble(FormattedCharSequence text) {
        if (!active() || text == null) return text;
        return sink -> text.accept((index, style, cp) -> sink.accept(index, style, scramble(cp)));
    }
}
