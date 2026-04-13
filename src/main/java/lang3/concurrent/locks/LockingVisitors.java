package lang3.concurrent.locks;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lang3.builder.AbstractSupplier;
public class LockingVisitors {
    public static class LockVisitor<O, L> {
        public static class LVBuilder<O, L, B extends LVBuilder<O, L, B>> extends AbstractSupplier<LockVisitor<O, L>, B, RuntimeException> {
            L lock;
            O object;
            private Supplier<Lock> readLockSupplier;
            private Supplier<Lock> writeLockSupplier;
            public LVBuilder() {
            }
            @Override
            public LockVisitor<O, L> get() {
                return new LockVisitor<>(this);
            }
            public B setLock(final L lock) {
                this.lock = lock;
                return asThis();
            }
            public B setObject(final O object) {
                this.object = object;
                return asThis();
            }
            public B setReadLockSupplier(final Supplier<Lock> readLockSupplier) {
                this.readLockSupplier = readLockSupplier;
                return asThis();
            }
            public B setWriteLockSupplier(final Supplier<Lock> writeLockSupplier) {
                this.writeLockSupplier = writeLockSupplier;
                return asThis();
            }
        }
        private final L lock;
        private final O object;
        private final Supplier<Lock> readLockSupplier;
        private final Supplier<Lock> writeLockSupplier;
        private LockVisitor(final LVBuilder<O, L, ?> builder) {
            this.object = Objects.requireNonNull(builder.object, "object");
            this.lock = Objects.requireNonNull(builder.lock, "lock");
            this.readLockSupplier = Objects.requireNonNull(builder.readLockSupplier, "readLockSupplier");
            this.writeLockSupplier = Objects.requireNonNull(builder.writeLockSupplier, "writeLockSupplier");
        }
        protected LockVisitor(final O object, final L lock, final Supplier<Lock> readLockSupplier, final Supplier<Lock> writeLockSupplier) {
            this.object = Objects.requireNonNull(object, "object");
            this.lock = Objects.requireNonNull(lock, "lock");
            this.readLockSupplier = Objects.requireNonNull(readLockSupplier, "readLockSupplier");
            this.writeLockSupplier = Objects.requireNonNull(writeLockSupplier, "writeLockSupplier");
        }
        public L getLock() {
            return lock;
        }
        public O getObject() {
            return object;
        }
    }
    public static class ReadWriteLockVisitor<O> extends LockVisitor<O, ReadWriteLock> {
        public static class Builder<O> extends LVBuilder<O, ReadWriteLock, Builder<O>> {
            public Builder() {
            }
            @Override
            public ReadWriteLockVisitor<O> get() {
                return new ReadWriteLockVisitor<>(this);
            }
            @Override
            public Builder<O> setLock(final ReadWriteLock readWriteLock) {
                setReadLockSupplier(readWriteLock::readLock);
                setWriteLockSupplier(readWriteLock::writeLock);
                return super.setLock(readWriteLock);
            }
        }
        public static <O> Builder<O> builder() {
            return new Builder<>();
        }
        private ReadWriteLockVisitor(final Builder<O> builder) {
            super(builder);
        }
        protected ReadWriteLockVisitor(final O object, final ReadWriteLock readWriteLock) {
            super(object, readWriteLock, readWriteLock::readLock, readWriteLock::writeLock);
        }
    }
    public static class ReentrantLockVisitor<O> extends LockVisitor<O, ReentrantLock> {
        public static class Builder<O> extends LVBuilder<O, ReentrantLock, Builder<O>> {
            public Builder() {
            }
            @Override
            public ReentrantLockVisitor<O> get() {
                return new ReentrantLockVisitor<>(this);
            }
            @Override
            public Builder<O> setLock(final ReentrantLock reentrantLock) {
                setReadLockSupplier(() -> reentrantLock);
                setWriteLockSupplier(() -> reentrantLock);
                return super.setLock(reentrantLock);
            }
        }
        public static <O> Builder<O> builder() {
            return new Builder<>();
        }
        private ReentrantLockVisitor(final Builder<O> builder) {
            super(builder);
        }
        protected ReentrantLockVisitor(final O object, final ReentrantLock reentrantLock) {
            super(object, reentrantLock, () -> reentrantLock, () -> reentrantLock);
        }
    }
    public static <O> ReadWriteLockVisitor<O> create(final O object, final ReadWriteLock readWriteLock) {
        return new ReadWriteLockVisitor<>(object, readWriteLock);
    }
    public static <O> ReentrantLockVisitor<O> create(final O object, final ReentrantLock reentrantLock) {
        return new ReentrantLockVisitor<>(object, reentrantLock);
    }
    @Deprecated
    public LockingVisitors() {
    }
}