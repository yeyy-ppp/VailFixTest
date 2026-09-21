package csv;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
abstract class AbstractStreamBuilder<T, B extends AbstractStreamBuilder<T, B>> {
    private Reader reader;
    private Charset charset;
    protected AbstractStreamBuilder() {
        this.charset = Charset.defaultCharset();
    }
    public abstract T get() throws IOException;
    @SuppressWarnings("unchecked")
    protected B asThis() {
        return (B) this;
    }
    protected Reader getReader() throws IOException {
        if (reader == null) {
            throw new IllegalStateException("Reader not set");
        }
        return reader;
    }
    protected Charset getCharset() {
        return charset;
    }
    public B setReader(final Reader reader) {
        this.reader = reader;
        return asThis();
    }
    public B setCharset(final Charset charset) {
        this.charset = charset != null ? charset : Charset.defaultCharset();
        return asThis();
    }
    public B setFile(final File file) throws IOException {
        this.reader = Files.newBufferedReader(file.toPath(), charset);
        return asThis();
    }
    public B setPath(final Path path) throws IOException {
        this.reader = Files.newBufferedReader(path, charset);
        return asThis();
    }
    public B setInputStream(final InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        return asThis();
    }
}