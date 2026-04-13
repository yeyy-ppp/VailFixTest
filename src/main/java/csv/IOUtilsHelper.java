package csv;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
final class IOUtilsHelper {
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static void copy(final InputStream input, final OutputStream output) throws IOException {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }
    public static void copy(final Reader input, final Appendable output) throws IOException {
        final char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.append(CharBuffer.wrap(buffer, 0, n));
        }
    }
    public static long copyLarge(final Reader input, final Writer output) throws IOException {
        final char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    private IOUtilsHelper() {
    }
    private static class CharBuffer implements CharSequence {
        private final char[] buffer;
        private final int offset;
        private final int length;
        CharBuffer(final char[] buffer, final int offset, final int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }
        static CharBuffer wrap(final char[] buffer, final int offset, final int length) {
            return new CharBuffer(buffer, offset, length);
        }
        @Override
        public int length() {
            return length;
        }
        @Override
        public char charAt(final int index) {
            if (index < 0 || index >= length) {
                throw new IndexOutOfBoundsException();
            }
            return buffer[offset + index];
        }
        @Override
        public CharSequence subSequence(final int start, final int end) {
            return new CharBuffer(buffer, offset + start, end - start);
        }
        @Override
        public String toString() {
            return new String(buffer, offset, length);
        }
    }
}