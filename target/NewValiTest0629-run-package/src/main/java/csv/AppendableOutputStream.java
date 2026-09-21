package csv;
import java.io.IOException;
import java.io.OutputStream;
final class AppendableOutputStream<T extends Appendable> extends OutputStream {
    private final T appendable;
    public AppendableOutputStream(final T appendable) {
        this.appendable = appendable;
    }
    @Override
    public void write(final int b) throws IOException {
        appendable.append((char) (b & 0xFF));
    }
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }
}