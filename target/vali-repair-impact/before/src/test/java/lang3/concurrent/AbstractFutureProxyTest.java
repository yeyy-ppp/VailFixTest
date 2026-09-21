package lang3.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbstractFutureProxyTest {

    @Test
    void testConstructor() {
        FutureTask<String> task = new FutureTask<>(() -> "test");
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        assertEquals(task, proxy.getFuture());
    }

    @Test
    void testCancel() {
        FutureTask<String> task = new FutureTask<>(() -> "test");
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        
        assertFalse(proxy.isCancelled());
        boolean cancelResult = proxy.cancel(true);
        assertTrue(cancelResult);
        assertTrue(proxy.isCancelled());
        assertTrue(proxy.isDone());
    }

    @Test
    void testGet() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> "result");
        task.run();
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        
        assertEquals("result", proxy.get());
        assertTrue(proxy.isDone());
    }

    @Test
    void testGetWithTimeout() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> {
            Thread.sleep(100);
            return "result";
        });
        new Thread(task).start();
        
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        assertEquals("result", proxy.get(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void testGetTimeoutException() {
        FutureTask<String> task = new FutureTask<>(() -> {
            Thread.sleep(200);
            return "result";
        });
        new Thread(task).start();
        
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        assertThrows(TimeoutException.class, () -> 
            proxy.get(50, TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testGetCancellationException() {
        FutureTask<String> task = new FutureTask<>(() -> "test");
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        proxy.cancel(true);
        
        assertThrows(CancellationException.class, () -> 
            proxy.get()
        );
        assertThrows(CancellationException.class, () -> 
            proxy.get(100, TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testIsDone() {
        FutureTask<String> task = new FutureTask<>(() -> "test");
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        
        assertFalse(proxy.isDone());
        task.run();
        assertTrue(proxy.isDone());
    }

    @Test
    void testIsCancelled() {
        FutureTask<String> task = new FutureTask<>(() -> "test");
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        
        assertFalse(proxy.isCancelled());
        proxy.cancel(false);
        assertTrue(proxy.isCancelled());
    }

    @Test
    void testGetFuture() {
        FutureTask<String> task = new FutureTask<>(() -> "test");
        AbstractFutureProxy<String> proxy = new AbstractFutureProxy<String>(task) {};
        
        assertSame(task, proxy.getFuture());
        
        task.run();
        assertSame(task, proxy.getFuture());
    }
}
