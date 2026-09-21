package lang3.concurrent;

import org.junit.jupiter.api.Test;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;

public class FutureTasksTest {

    @Test
    void testRunWithNormalCallable() throws Exception {
        Callable<String> callable = () -> "TestResult";
        FutureTask<String> task = FutureTasks.run(callable);

        assertTrue(task.isDone());
        assertEquals("TestResult", task.get());
    }

    @Test
    void testRunWithExceptionThrowingCallable() {
        RuntimeException expectedException = new RuntimeException("Expected error");
        Callable<Void> callable = () -> { throw expectedException; };
        FutureTask<Void> task = FutureTasks.run(callable);

        assertTrue(task.isDone());
        ExecutionException actualException = assertThrows(ExecutionException.class, () -> task.get());
        assertSame(expectedException, actualException.getCause());
    }

    @Test
    void testConstructor() throws Exception {
        Constructor<FutureTasks> constructor = FutureTasks.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

}