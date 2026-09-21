package csv;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
final class Base64OutputStream extends FilterOutputStream {
    private final Base64.Encoder encoder;
    private final OutputStream wrappedStream;
    private byte[] buffer;
    private int bufferLength;
    private static final int BUFFER_SIZE = 3;
    public Base64OutputStream(final OutputStream out) {
        super(out);
        this.encoder = Base64.getEncoder();
        this.wrappedStream = out;
        this.buffer = new byte[BUFFER_SIZE];
        this.bufferLength = 0;
    }
    @Override
    public void write(final int b) throws IOException {
        buffer[bufferLength++] = (byte) b;
        if (bufferLength == BUFFER_SIZE) {
            encodeAndWrite();
        }
    }
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }
    private void encodeAndWrite() throws IOException {
        if (bufferLength > 0) {
            final byte[] encoded = encoder.encode(java.util.Arrays.copyOf(buffer, bufferLength));
            wrappedStream.write(encoded);
            bufferLength = 0;
        }
    }
    @Override
    public void flush() throws IOException {
        encodeAndWrite();
        super.flush();
    }
    @Override
    public void close() throws IOException {
        try {
            encodeAndWrite();
        } finally {
            super.close();
        }
    }
}