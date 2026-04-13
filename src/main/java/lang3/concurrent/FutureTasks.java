package lang3.concurrent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
public class FutureTasks {
    public static <V> FutureTask<V> run(final Callable<V> callable) {
        final FutureTask<V> futureTask = new FutureTask<>(callable);
        futureTask.run();
        return futureTask;
    }
    private FutureTasks() {
    }
}