package lang3.concurrent;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
public class BasicThreadFactory implements ThreadFactory {
    public static class Builder implements lang3.builder.Builder<BasicThreadFactory> {
        private ThreadFactory factory;
        private UncaughtExceptionHandler exceptionHandler;
        private String namingPattern;
        private Integer priority;
        private Boolean daemon;
        @Deprecated
        public Builder() {
        }
        @Override
        public BasicThreadFactory build() {
            final BasicThreadFactory factory = new BasicThreadFactory(this);
            reset();
            return factory;
        }
        public Builder priority(final int priority) {
            this.priority = Integer.valueOf(priority);
            return this;
        }
        public void reset() {
            factory = null;
            exceptionHandler = null;
            namingPattern = null;
            priority = null;
            daemon = null;
        }
    }
    public static Builder builder() {
        return new Builder();
    }
    private final AtomicLong threadCounter;
    private final ThreadFactory wrappedFactory;
    private final UncaughtExceptionHandler uncaughtExceptionHandler;
    private final String namingPattern;
    private final Integer priority;
    private final Boolean daemon;
    private BasicThreadFactory(final Builder builder) {
        wrappedFactory = builder.factory != null ? builder.factory : Executors.defaultThreadFactory();
        namingPattern = builder.namingPattern;
        priority = builder.priority;
        daemon = builder.daemon;
        uncaughtExceptionHandler = builder.exceptionHandler;
        threadCounter = new AtomicLong();
    }
    public final Boolean getDaemonFlag() {
        return daemon;
    }
    public final String getNamingPattern() {
        return namingPattern;
    }
    public final Integer getPriority() {
        return priority;
    }
    public final UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }
    public final ThreadFactory getWrappedFactory() {
        return wrappedFactory;
    }
    private void initializeThread(final Thread thread) {
        if (getNamingPattern() != null) {
            final Long count = Long.valueOf(threadCounter.incrementAndGet());
            thread.setName(String.format(getNamingPattern(), count));
        }
        if (getUncaughtExceptionHandler() != null) {
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler());
        }
        if (getPriority() != null) {
            thread.setPriority(getPriority().intValue());
        }
        if (getDaemonFlag() != null) {
            thread.setDaemon(getDaemonFlag().booleanValue());
        }
    }
    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = getWrappedFactory().newThread(runnable);
        initializeThread(thread);
        return thread;
    }
}