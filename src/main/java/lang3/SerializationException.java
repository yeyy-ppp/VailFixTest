package lang3;
public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = 4029025366392702726L;
    public SerializationException() {
    }
    public SerializationException(final String msg) {
        super(msg);
    }
    public SerializationException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
    public SerializationException(final Throwable cause) {
        super(cause);
    }
}