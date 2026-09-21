package lang3.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class UncheckedFutureTest {

    @Test
    void testMap() {
        Collection<Future<String>> futures = new ArrayList<>();
        futures.add(createCompletedFuture("result1"));
        futures.add(createCompletedFuture("result2"));

        Stream<UncheckedFuture<String>> stream = UncheckedFuture.map(futures);
        assertEquals(2, stream.count());
    }

    @Test
    void testMapWithEmptyCollection() {
        Collection<Future<String>> emptyFutures = Collections.emptyList();
        Stream<UncheckedFuture<String>> stream = UncheckedFuture.map(emptyFutures);
        assertEquals(0, stream.count());
    }

    @Test
    void testOnCollection() {
        Collection<Future<String>> futures = new ArrayList<>();
        futures.add(createCompletedFuture("test"));
        futures.add(createCompletedFuture("test"));

        Collection<UncheckedFuture<String>> uncheckedFutures = UncheckedFuture.on(futures);
        assertEquals(2, uncheckedFutures.size());
        uncheckedFutures.forEach(future -> assertTrue(future instanceof UncheckedFuture));
    }

    @Test
    void testOnSingle() {
        Future<String> future = createCompletedFuture("success");
        UncheckedFuture<String> uncheckedFuture = UncheckedFuture.on(future);
        assertNotNull(uncheckedFuture);
    }

    @Test
    void testGetSuccess() throws Exception {
        Future<String> future = createCompletedFuture("value");
        UncheckedFuture<String> uncheckedFuture = UncheckedFuture.on(future);
        assertEquals("value", uncheckedFuture.get());
    }

    @Test
    void testGetException() {
        Future<String> future = createFailedFuture(new RuntimeException("error"));
        UncheckedFuture<String> uncheckedFuture = UncheckedFuture.on(future);

        UncheckedExecutionException uncheckedExecutionException = assertThrows(UncheckedExecutionException.class, uncheckedFuture::get);
        assertFalse(uncheckedExecutionException.getCause() instanceof RuntimeException);
        assertEquals("java.lang.RuntimeException: error", uncheckedExecutionException.getCause().getMessage());
    }

    @Test
    void testGetCancelled() {
        Future<String> future = createCancelledFuture();
        UncheckedFuture<String> uncheckedFuture = UncheckedFuture.on(future);

        assertThrows(CancellationException.class, uncheckedFuture::get);
    }

    @Test
    void testGetWithTimeoutSuccess() throws Exception {
        Future<String> future = createCompletedFuture("result");
        UncheckedFuture<String> uncheckedFuture = UncheckedFuture.on(future);
        assertEquals("result", uncheckedFuture.get(100, TimeUnit.MILLISECONDS));
    }

   /* @Test
    void testGetWithTimeoutException() {
        Future<String> future = createFailedFuture(new IllegalStateException("failure"));
        UncheckedFuture<String> uncheckedFuture = UncheckedFuture.on(future);

        ExecutionException exception = assertThrows(ExecutionException.class,
            () -> uncheckedFuture.get(100, TimeUnit.MILLISECONDS));
        assertTrue(exception.getCause() instanceof IllegalStateException);
    }*/

    @Test
    @Timeout(1)
    void testGetWithTimeoutExpired() {
        Future<String> future = createIncompleteFuture();
        UncheckedFuture<String> uncheckedFuture = UncheckedFuture.on(future);

        assertThrows(UncheckedTimeoutException.class,
            () -> uncheckedFuture.get(100, TimeUnit.MILLISECONDS));
    }

    private <T> Future<T> createCompletedFuture(T result) {
        return new MockFuture<>(result, null, false, true);
    }

    private <T> Future<T> createFailedFuture(Throwable exception) {
        return new MockFuture<>(null, exception, false, true);
    }

    private <T> Future<T> createCancelledFuture() {
        return new MockFuture<>(null, null, true, false);
    }

    private <T> Future<T> createIncompleteFuture() {
        return new MockFuture<>(null, null, false, false);
    }

    private static class MockFuture<V> implements Future<V> {
        private final V result;
        private final Throwable exception;
        private final boolean cancelled;
        private final boolean done;

        MockFuture(V result, Throwable exception, boolean cancelled, boolean done) {
            this.result = result;
            this.exception = exception;
            this.cancelled = cancelled;
            this.done = done;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            if (exception != null) throw new ExecutionException(exception);
            if (cancelled) throw new CancellationException();
            return result;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (!done) throw new TimeoutException();
            if (exception != null) throw new ExecutionException(exception);
            if (cancelled) throw new CancellationException();
            return result;
        }
    }
}