package csv;
import java.nio.charset.Charset;
final class Charsets {
    public static Charset toCharset(final Charset charset) {
        return charset != null ? charset : Charset.defaultCharset();
    }
    public static Charset toCharset(final String charsetName) {
        return charsetName != null ? Charset.forName(charsetName) : Charset.defaultCharset();
    }
    private Charsets() {
    }
}